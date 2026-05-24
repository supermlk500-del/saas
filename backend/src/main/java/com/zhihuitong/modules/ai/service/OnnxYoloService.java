package com.zhihuitong.modules.ai.service;

import ai.onnxruntime.NodeInfo;
import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.ai.config.AiYoloProperties;
import com.zhihuitong.modules.ai.model.YoloBox;
import com.zhihuitong.modules.ai.model.YoloDetectResult;
import com.zhihuitong.modules.ai.util.ImagePreprocessUtils;
import com.zhihuitong.modules.quality.service.InspectionFileStorageService;
import com.zhihuitong.modules.quality.vo.StoredInspectionFile;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.FloatBuffer;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class OnnxYoloService {

    private static final Pattern LABEL_PATTERN = Pattern.compile("(\\d+)\\s*:\\s*'([^']+)'");
    private static final Logger log = LoggerFactory.getLogger(OnnxYoloService.class);

    private final AiYoloProperties properties;
    private final InspectionFileStorageService inspectionFileStorageService;

    private final Object modelLock = new Object();
    private volatile OrtEnvironment environment;
    private volatile OrtSession session;
    private volatile String inputName;
    private volatile int inputWidth;
    private volatile int inputHeight;
    private volatile Map<Integer, String> labelMap = Collections.emptyMap();

    public OnnxYoloService(AiYoloProperties properties,
                           InspectionFileStorageService inspectionFileStorageService) {
        this.properties = properties;
        this.inspectionFileStorageService = inspectionFileStorageService;
    }

    public YoloDetectResult detect(Path sourceImagePath, String sourceRelativePath, String inspectType) {
        if (!properties.isEnabled()) {
            throw new BusinessException(422, "Local YOLO detection is disabled");
        }
        ensureModelLoaded();
        BufferedImage sourceImage = readImage(sourceImagePath);
        return detect(sourceImage, inspectType, sourceRelativePath, true);
    }

    public YoloDetectResult detectRealtime(byte[] frameBytes, String inspectType) {
        if (!properties.isEnabled()) {
            throw new BusinessException(422, "Local YOLO detection is disabled");
        }
        ensureModelLoaded();
        BufferedImage sourceImage = readImage(frameBytes);
        return detect(sourceImage, inspectType, null, false);
    }

    private YoloDetectResult detect(BufferedImage sourceImage,
                                    String inspectType,
                                    String sourceRelativePath,
                                    boolean persistRenderedImage) {
        ImagePreprocessUtils.PreprocessResult preprocessResult = ImagePreprocessUtils.preprocess(sourceImage, inputWidth, inputHeight);
        DetectionProfile detectionProfile = resolveProfile(inspectType);
        log.info("ONNX detect start: inspectType={}, sourcePath={}, sourceImage={}x{}, modelInput={}x{}, confThreshold={}, iouThreshold={}, fallbackThreshold={}",
                inspectType,
                sourceRelativePath,
                sourceImage.getWidth(),
                sourceImage.getHeight(),
                inputWidth,
                inputHeight,
                detectionProfile.confThreshold(),
                detectionProfile.iouThreshold(),
                detectionProfile.fallbackConfThreshold());
        List<YoloBox> boxes = runInference(preprocessResult, detectionProfile, inspectType);
        boxes.sort(Comparator.comparingDouble(YoloBox::getScore).reversed());
        String resultJudge = judge(boxes, detectionProfile);

        YoloDetectResult result = new YoloDetectResult();
        result.setInspectType(StringUtils.hasText(inspectType) ? inspectType : "video");
        result.setResultJudge(resultJudge);
        result.setConfidenceScore(topConfidence(boxes));
        result.setDefectType(topDefectType(boxes));
        result.setResultValue(buildResultValue(boxes));
        result.setSourceImageUrl(sourceRelativePath);
        result.setBoxes(boxes);
        if (persistRenderedImage) {
            StoredInspectionFile resultTarget = inspectionFileStorageService.prepareResultImageTarget(".jpg");
            BufferedImage renderedImage = ImagePreprocessUtils.renderDetections(sourceImage, boxes, resultJudge);
            inspectionFileStorageService.writeRenderedImage(renderedImage, resultTarget);
            result.setImageUrl(resultTarget.getRelativePath());
        }
        log.info("ONNX detect finish: inspectType={}, resultJudge={}, boxCount={}, topScore={}",
                inspectType,
                resultJudge,
                boxes.size(),
                boxes.isEmpty() ? null : boxes.get(0).getScore());
        return result;
    }

    private void ensureModelLoaded() {
        if (session != null) {
            return;
        }
        synchronized (modelLock) {
            if (session != null) {
                return;
            }
            try {
                environment = OrtEnvironment.getEnvironment();
                OrtSession.SessionOptions sessionOptions = new OrtSession.SessionOptions();
                session = environment.createSession(properties.getModelPath(), sessionOptions);
                inputName = session.getInputNames().iterator().next();
                NodeInfo nodeInfo = session.getInputInfo().get(inputName);
                TensorInfo tensorInfo = (TensorInfo) nodeInfo.getInfo();
                long[] shape = tensorInfo.getShape();
                inputHeight = resolveDimension(shape, 2, 640);
                inputWidth = resolveDimension(shape, 3, 640);
                labelMap = loadLabels(session);
            } catch (OrtException exception) {
                throw new BusinessException(500, "Failed to initialize ONNX model: " + exception.getMessage());
            }
        }
    }

    private BufferedImage readImage(Path sourceImagePath) {
        try {
            BufferedImage image = ImageIO.read(sourceImagePath.toFile());
            if (image == null) {
                throw new BusinessException(400, "Uploaded file is not a valid image");
            }
            return image;
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to read uploaded inspection image: " + exception.getMessage());
        }
    }

    private BufferedImage readImage(byte[] frameBytes) {
        try (ByteArrayInputStream inputStream = new ByteArrayInputStream(frameBytes)) {
            BufferedImage image = ImageIO.read(inputStream);
            if (image == null) {
                throw new BusinessException(400, "Realtime frame is not a valid image");
            }
            return image;
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to decode realtime frame image: " + exception.getMessage());
        }
    }

    private List<YoloBox> runInference(ImagePreprocessUtils.PreprocessResult preprocessResult,
                                       DetectionProfile detectionProfile,
                                       String inspectType) {
        float[] inputData = preprocessResult.chw();
        long[] inputShape = new long[]{1, 3, inputHeight, inputWidth};
        try (OnnxTensor tensor = OnnxTensor.createTensor(environment, FloatBuffer.wrap(inputData), inputShape);
             OrtSession.Result output = session.run(Collections.singletonMap(inputName, tensor))) {
            Object value = output.get(0).getValue();
            log.info("ONNX raw output shape: inspectType={}, shape={}", inspectType, describeShape(value));
            DecodedPredictions decodedPredictions = decodePredictions(value, preprocessResult);
            List<RawPrediction> standardCandidates = decodedPredictions.predictions().stream()
                    .filter(item -> item.box.getScore() >= detectionProfile.confThreshold())
                    .toList();
            List<YoloBox> standardBoxes = nonMaximumSuppression(standardCandidates, detectionProfile.iouThreshold());
            log.info("ONNX candidate stats: inspectType={}, decodedCandidates={}, standardCandidates={}, standardBoxes={}, maxScore={}",
                    inspectType,
                    decodedPredictions.predictions().size(),
                    standardCandidates.size(),
                    standardBoxes.size(),
                    decodedPredictions.maxScore());
            if (!standardBoxes.isEmpty()) {
                return standardBoxes;
            }

            if ("video".equalsIgnoreCase(inspectType)) {
                List<RawPrediction> fallbackCandidates = decodedPredictions.predictions().stream()
                        .filter(item -> item.box.getScore() >= detectionProfile.fallbackConfThreshold())
                        .toList();
                List<YoloBox> fallbackBoxes = nonMaximumSuppression(fallbackCandidates, detectionProfile.iouThreshold());
                log.info("ONNX video fallback stats: inspectType={}, fallbackCandidates={}, fallbackBoxes={}, fallbackThreshold={}",
                        inspectType,
                        fallbackCandidates.size(),
                        fallbackBoxes.size(),
                        detectionProfile.fallbackConfThreshold());
                return fallbackBoxes;
            }
            return standardBoxes;
        } catch (OrtException exception) {
            throw new BusinessException(500, "ONNX inference failed: " + exception.getMessage());
        }
    }

    private DecodedPredictions decodePredictions(Object outputValue, ImagePreprocessUtils.PreprocessResult preprocessResult) {
        if (!(outputValue instanceof float[][][] tensor)) {
            throw new BusinessException(500, "Unsupported YOLO output tensor type: " + outputValue.getClass().getName());
        }
        float[][] candidates = toCandidateMatrix(tensor[0]);
        if (candidates.length == 0) {
            return new DecodedPredictions(Collections.emptyList(), 0.0F);
        }
        int featureCount = candidates[0].length;
        int classCount = Math.max(0, featureCount - 4);
        boolean hasObjectness = labelMap.size() > 0 && featureCount == labelMap.size() + 5;
        int classStart = hasObjectness ? 5 : 4;
        if (!labelMap.isEmpty()) {
            classCount = hasObjectness ? featureCount - 5 : featureCount - 4;
        }
        if (classCount <= 0) {
            throw new BusinessException(500, "YOLO output tensor does not contain any class logits");
        }

        List<RawPrediction> rawPredictions = new ArrayList<>();
        float maxScore = 0.0F;
        for (float[] candidate : candidates) {
            float objectness = hasObjectness ? candidate[4] : 1.0F;
            int bestClassIndex = -1;
            float bestClassScore = 0.0F;
            for (int classIndex = 0; classIndex < classCount; classIndex++) {
                float classScore = candidate[classStart + classIndex];
                if (classScore > bestClassScore) {
                    bestClassScore = classScore;
                    bestClassIndex = classIndex;
                }
            }
            float confidence = objectness * bestClassScore;
            maxScore = Math.max(maxScore, confidence);
            if (bestClassIndex < 0 || confidence < 0.01F) {
                continue;
            }
            YoloBox box = scaleToOriginal(candidate, preprocessResult, bestClassIndex, confidence);
            if (box.getX2() <= box.getX1() || box.getY2() <= box.getY1()) {
                continue;
            }
            rawPredictions.add(new RawPrediction(box));
        }
        return new DecodedPredictions(rawPredictions, maxScore);
    }

    private float[][] toCandidateMatrix(float[][] tensor) {
        if (tensor.length == 0) {
            return new float[0][0];
        }
        int first = tensor.length;
        int second = tensor[0].length;
        if (first > second) {
            return tensor;
        }
        float[][] transposed = new float[second][first];
        for (int i = 0; i < first; i++) {
            for (int j = 0; j < second; j++) {
                transposed[j][i] = tensor[i][j];
            }
        }
        return transposed;
    }

    private YoloBox scaleToOriginal(float[] candidate,
                                    ImagePreprocessUtils.PreprocessResult preprocessResult,
                                    int classIndex,
                                    float confidence) {
        float cx = candidate[0];
        float cy = candidate[1];
        float width = candidate[2];
        float height = candidate[3];
        float x1 = (cx - width / 2.0F - preprocessResult.padX()) / preprocessResult.scale();
        float y1 = (cy - height / 2.0F - preprocessResult.padY()) / preprocessResult.scale();
        float x2 = (cx + width / 2.0F - preprocessResult.padX()) / preprocessResult.scale();
        float y2 = (cy + height / 2.0F - preprocessResult.padY()) / preprocessResult.scale();

        YoloBox box = new YoloBox();
        box.setLabel(labelMap.getOrDefault(classIndex, "defect_" + classIndex));
        box.setScore(confidence);
        box.setX1(clamp(Math.round(x1), 0, preprocessResult.originalWidth() - 1));
        box.setY1(clamp(Math.round(y1), 0, preprocessResult.originalHeight() - 1));
        box.setX2(clamp(Math.round(x2), 0, preprocessResult.originalWidth() - 1));
        box.setY2(clamp(Math.round(y2), 0, preprocessResult.originalHeight() - 1));
        return box;
    }

    private List<YoloBox> nonMaximumSuppression(List<RawPrediction> predictions, float iouThreshold) {
        if (predictions.isEmpty()) {
            return Collections.emptyList();
        }
        List<RawPrediction> sorted = predictions.stream()
                .sorted(Comparator.comparingDouble((RawPrediction item) -> item.box.getScore()).reversed())
                .collect(Collectors.toCollection(ArrayList::new));
        List<YoloBox> result = new ArrayList<>();
        boolean[] removed = new boolean[sorted.size()];
        for (int i = 0; i < sorted.size(); i++) {
            if (removed[i]) {
                continue;
            }
            RawPrediction current = sorted.get(i);
            result.add(current.box);
            for (int j = i + 1; j < sorted.size(); j++) {
                if (removed[j]) {
                    continue;
                }
                RawPrediction next = sorted.get(j);
                if (!current.box.getLabel().equals(next.box.getLabel())) {
                    continue;
                }
                if (iou(current.box, next.box) >= iouThreshold) {
                    removed[j] = true;
                }
            }
        }
        return result;
    }

    private float iou(YoloBox a, YoloBox b) {
        int left = Math.max(a.getX1(), b.getX1());
        int top = Math.max(a.getY1(), b.getY1());
        int right = Math.min(a.getX2(), b.getX2());
        int bottom = Math.min(a.getY2(), b.getY2());
        int intersectionWidth = Math.max(0, right - left);
        int intersectionHeight = Math.max(0, bottom - top);
        float intersection = intersectionWidth * intersectionHeight;
        float areaA = Math.max(0, a.getX2() - a.getX1()) * Math.max(0, a.getY2() - a.getY1());
        float areaB = Math.max(0, b.getX2() - b.getX1()) * Math.max(0, b.getY2() - b.getY1());
        float union = areaA + areaB - intersection;
        return union <= 0 ? 0 : intersection / union;
    }

    private int resolveDimension(long[] shape, int index, int defaultValue) {
        if (shape.length <= index) {
            return defaultValue;
        }
        long dimension = shape[index];
        if (dimension <= 0 || dimension > Integer.MAX_VALUE) {
            return defaultValue;
        }
        return (int) dimension;
    }

    private Map<Integer, String> loadLabels(OrtSession ortSession) throws OrtException {
        Map<Integer, String> labels = new LinkedHashMap<>();
        for (Map.Entry<String, String> entry : ortSession.getMetadata().getCustomMetadata().entrySet()) {
            if (!"names".equalsIgnoreCase(entry.getKey())) {
                continue;
            }
            Matcher matcher = LABEL_PATTERN.matcher(entry.getValue());
            while (matcher.find()) {
                labels.put(Integer.parseInt(matcher.group(1)), matcher.group(2));
            }
        }
        return labels.isEmpty() ? defaultLabels() : labels;
    }

    private Map<Integer, String> defaultLabels() {
        Map<Integer, String> labels = new HashMap<>();
        labels.put(0, "hole");
        labels.put(1, "stain");
        labels.put(2, "three_silk");
        labels.put(3, "knot");
        labels.put(4, "broken_end");
        labels.put(5, "jump");
        return labels;
    }

    private String judge(List<YoloBox> boxes, DetectionProfile detectionProfile) {
        if (boxes.isEmpty()) {
            return "PASS";
        }
        double topScore = boxes.get(0).getScore();
        return topScore >= detectionProfile.failThreshold() ? "FAIL" : "RECHECK";
    }

    private BigDecimal topConfidence(List<YoloBox> boxes) {
        if (boxes.isEmpty()) {
            return null;
        }
        return BigDecimal.valueOf(boxes.get(0).getScore());
    }

    private String topDefectType(List<YoloBox> boxes) {
        if (boxes.isEmpty()) {
            return null;
        }
        return boxes.get(0).getLabel();
    }

    private String buildResultValue(List<YoloBox> boxes) {
        if (boxes.isEmpty()) {
            return "PASS";
        }
        return boxes.stream()
                .limit(3)
                .map(item -> item.getLabel() + ":" + String.format(Locale.US, "%.2f", item.getScore()))
                .collect(Collectors.joining(","));
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private DetectionProfile resolveProfile(String inspectType) {
        if ("video".equalsIgnoreCase(inspectType)) {
            float standard = properties.getVideoConfThreshold();
            float fallback = Math.min(standard, properties.getVideoFallbackConfThreshold());
            float failThreshold = Math.max(standard, 0.40F);
            return new DetectionProfile(standard, fallback, properties.getVideoIouThreshold(), failThreshold);
        }
        float standard = properties.getConfThreshold();
        float failThreshold = Math.max(standard, 0.50F);
        return new DetectionProfile(standard, standard, properties.getIouThreshold(), failThreshold);
    }

    private String describeShape(Object value) {
        if (value instanceof float[][][] tensor3) {
            return Arrays.toString(new int[]{tensor3.length, tensor3[0].length, tensor3[0][0].length});
        }
        if (value instanceof float[][][][] tensor4) {
            return Arrays.toString(new int[]{tensor4.length, tensor4[0].length, tensor4[0][0].length, tensor4[0][0][0].length});
        }
        return value.getClass().getSimpleName();
    }

    private record RawPrediction(YoloBox box) {
    }

    private record DecodedPredictions(List<RawPrediction> predictions, float maxScore) {
    }

    private record DetectionProfile(float confThreshold,
                                    float fallbackConfThreshold,
                                    float iouThreshold,
                                    float failThreshold) {
    }
}
