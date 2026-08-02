package com.zhihuitong.modules.ai.service;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxJavaType;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.ai.config.AiYoloProperties;
import com.zhihuitong.modules.ai.dto.BrowserInferenceClassItem;
import com.zhihuitong.modules.ai.dto.BrowserInferenceDecoder;
import com.zhihuitong.modules.ai.dto.BrowserInferenceManifestResponse;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BrowserInferenceModelService {

    private static final Pattern LABEL_PATTERN = Pattern.compile(
            "(\\d+)\\s*:\\s*(?:'([^']+)'|\"([^\"]+)\")"
    );
    private static final String[] DEFAULT_COLORS = {
            "#ef4444", "#f97316", "#eab308", "#22c55e", "#06b6d4", "#8b5cf6"
    };

    private final AiYoloProperties properties;

    private final Object cacheLock = new Object();
    private volatile CachedManifest cachedManifest;

    public BrowserInferenceModelService(AiYoloProperties properties) {
        this.properties = properties;
    }

    public BrowserInferenceManifestResponse getManifest() {
        CachedManifest cache = getCachedManifest();
        return cache.manifest();
    }

    public Resource getModelResource(String requestedSha256) {
        CachedManifest cache = getCachedManifest();
        if (!cache.sha256().equalsIgnoreCase(requestedSha256)) {
            throw new BusinessException(409, "Requested model sha256 is not the active model");
        }
        return new FileSystemResource(cache.path());
    }

    public long getModelFileSize() {
        return getCachedManifest().fileSize();
    }

    public String getActiveSha256() {
        return getCachedManifest().sha256();
    }

    private CachedManifest getCachedManifest() {
        Path modelPath = resolveModelPath();
        long size = fileSize(modelPath);
        long lastModified = lastModified(modelPath);
        CachedManifest cache = cachedManifest;
        if (cache != null && cache.path().equals(modelPath) && cache.fileSize() == size && cache.lastModified() == lastModified) {
            return cache;
        }
        synchronized (cacheLock) {
            cache = cachedManifest;
            if (cache != null && cache.path().equals(modelPath) && cache.fileSize() == size && cache.lastModified() == lastModified) {
                return cache;
            }
            CachedManifest refreshed = inspectModel(modelPath, size, lastModified);
            cachedManifest = refreshed;
            return refreshed;
        }
    }

    private CachedManifest inspectModel(Path modelPath, long size, long lastModified) {
        String sha256 = sha256(modelPath);
        OrtEnvironment environment = OrtEnvironment.getEnvironment();
        try (OrtSession session = environment.createSession(modelPath.toString(), new OrtSession.SessionOptions())) {
            if (session.getInputNames().size() != 1 || session.getOutputNames().size() != 1) {
                throw new BusinessException(500, "Browser ONNX model must expose exactly one input and one output");
            }
            String inputName = session.getInputNames().iterator().next();
            String outputName = session.getOutputNames().iterator().next();
            TensorInfo inputInfo = tensorInfo(session.getInputInfo().get(inputName));
            TensorInfo outputInfo = tensorInfo(session.getOutputInfo().get(outputName));
            if (inputInfo.type != OnnxJavaType.FLOAT || outputInfo.type != OnnxJavaType.FLOAT) {
                throw new BusinessException(500, "Browser ONNX model input and output must be float32 tensors");
            }
            long[] inputShape = inputInfo.getShape();
            if (inputShape.length != 4 || inputShape[0] != 1 || inputShape[1] != 3) {
                throw new BusinessException(500, "Browser ONNX model input must use static NCHW shape [1,3,H,W]");
            }
            Map<Integer, String> labels = loadLabels(session);

            BrowserInferenceManifestResponse manifest = new BrowserInferenceManifestResponse();
            manifest.setModelName(modelPath.getFileName().toString());
            manifest.setModelVersion(sha256.substring(0, 12));
            manifest.setModelUrl("/api/ai/browser-inference/model?sha256=" + sha256);
            manifest.setSha256(sha256);
            manifest.setFileSize(size);
            manifest.setInputName(inputName);
            manifest.setOutputName(outputName);
            manifest.setInputHeight(requireStaticDimension(inputShape, 2, "height"));
            manifest.setInputWidth(requireStaticDimension(inputShape, 3, "width"));
            manifest.setDecoder(resolveDecoder(session, labels));
            manifest.setClasses(toClassItems(labels));
            manifest.setConfidenceThreshold(properties.getConfThreshold());
            manifest.setIouThreshold(properties.getIouThreshold());
            manifest.setRealtimeConfidenceThreshold(properties.getVideoConfThreshold());
            return new CachedManifest(modelPath, size, lastModified, sha256, manifest);
        } catch (OrtException exception) {
            throw new BusinessException(500, "Failed to inspect browser inference model: " + exception.getMessage());
        }
    }

    private BrowserInferenceDecoder resolveDecoder(OrtSession session, Map<Integer, String> labels) throws OrtException {
        BrowserInferenceDecoder decoder = new BrowserInferenceDecoder();
        String outputName = session.getOutputNames().iterator().next();
        TensorInfo outputInfo = tensorInfo(session.getOutputInfo().get(outputName));
        long[] shape = outputInfo.getShape();
        if (shape.length != 3 || shape[0] != 1 || shape[1] <= 0 || shape[2] <= 0) {
            throw new BusinessException(500, "Browser ONNX output must use static rank-3 shape [1,F,N] or [1,N,F]");
        }

        long first = shape[1];
        long second = shape[2];
        int classCount = labels.size();
        if (first == 6 && second != 6) {
            decoder.setType("nms-xyxy6");
            decoder.setOutputLayout("BCN");
            decoder.setBoxFormat("xyxy");
            return decoder;
        }
        if (second == 6 && first != 6) {
            decoder.setType("nms-xyxy6");
            decoder.setOutputLayout("BNC");
            decoder.setBoxFormat("xyxy");
            return decoder;
        }
        if ((first == classCount + 4 || first == classCount + 5)
                && second != classCount + 4 && second != classCount + 5) {
            decoder.setType("yolo-raw");
            decoder.setOutputLayout("BCN");
            decoder.setHasObjectness(first == classCount + 5);
            return decoder;
        }
        if ((second == classCount + 4 || second == classCount + 5)
                && first != classCount + 4 && first != classCount + 5) {
            decoder.setType("yolo-raw");
            decoder.setOutputLayout("BNC");
            decoder.setHasObjectness(second == classCount + 5);
            return decoder;
        }
        throw new BusinessException(
                500,
                "Unsupported browser ONNX output shape [" + shape[0] + "," + first + "," + second
                        + "] for " + classCount + " classes"
        );
    }

    private TensorInfo tensorInfo(NodeInfo nodeInfo) {
        if (!(nodeInfo.getInfo() instanceof TensorInfo tensorInfo)) {
            throw new BusinessException(500, "ONNX model input/output is not a tensor");
        }
        return tensorInfo;
    }

    private Map<Integer, String> loadLabels(OrtSession session) throws OrtException {
        Map<Integer, String> parsed = new TreeMap<>();
        for (Map.Entry<String, String> entry : session.getMetadata().getCustomMetadata().entrySet()) {
            if (!"names".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            Matcher matcher = LABEL_PATTERN.matcher(entry.getValue());
            while (matcher.find()) {
                String label = matcher.group(2) != null ? matcher.group(2) : matcher.group(3);
                parsed.put(Integer.parseInt(matcher.group(1)), label);
            }
        }
        if (parsed.isEmpty()) {
            throw new BusinessException(500, "Browser ONNX model metadata must define the names class mapping");
        }
        Map<Integer, String> labels = new LinkedHashMap<>();
        for (int index = 0; index < parsed.size(); index++) {
            String label = parsed.get(index);
            if (!StringUtils.hasText(label)) {
                throw new BusinessException(500, "Browser ONNX class indexes must be contiguous from zero");
            }
            labels.put(index, label);
        }
        return labels;
    }

    private List<BrowserInferenceClassItem> toClassItems(Map<Integer, String> labels) {
        List<BrowserInferenceClassItem> result = new ArrayList<>();
        labels.forEach((index, label) ->
                result.add(new BrowserInferenceClassItem(index, label, label, DEFAULT_COLORS[index % DEFAULT_COLORS.length]))
        );
        return result;
    }

    private Path resolveModelPath() {
        if (!StringUtils.hasText(properties.getModelPath())) {
            throw new BusinessException(500, "ai.yolo.model-path must not be blank");
        }
        Path modelPath = Path.of(properties.getModelPath()).toAbsolutePath().normalize();
        if (!Files.isRegularFile(modelPath)) {
            throw new BusinessException(500, "ONNX model file does not exist: " + modelPath);
        }
        return modelPath;
    }

    private int requireStaticDimension(long[] shape, int index, String dimensionName) {
        if (shape.length <= index || shape[index] <= 0 || shape[index] > Integer.MAX_VALUE) {
            throw new BusinessException(500, "Browser ONNX input " + dimensionName + " must be a positive static dimension");
        }
        return (int) shape[index];
    }

    private long fileSize(Path path) {
        try {
            return Files.size(path);
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to read model file size: " + exception.getMessage());
        }
    }

    private long lastModified(Path path) {
        try {
            return Files.getLastModifiedTime(path).toMillis();
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to read model lastModified: " + exception.getMessage());
        }
    }

    private String sha256(Path path) {
        try (InputStream inputStream = Files.newInputStream(path)) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int read;
            while ((read = inputStream.read(buffer)) >= 0) {
                digest.update(buffer, 0, read);
            }
            byte[] bytes = digest.digest();
            StringBuilder builder = new StringBuilder(bytes.length * 2);
            for (byte item : bytes) {
                builder.append(String.format("%02x", item));
            }
            return builder.toString();
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new BusinessException(500, "Failed to calculate model sha256: " + exception.getMessage());
        }
    }

    private record CachedManifest(Path path,
                                  long fileSize,
                                  long lastModified,
                                  String sha256,
                                  BrowserInferenceManifestResponse manifest) {
    }
}
