package com.zhihuitong.modules.ai.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.ProjectPathResolver;
import com.zhihuitong.modules.ai.config.AiYoloProperties;
import com.zhihuitong.modules.ai.dto.BrowserInferenceManifestResponse;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Service
public class BrowserInferenceModelService {

    private static final String MANIFEST_RESOURCE = "ai/browser-inference-manifest.json";
    private static final int HASH_BUFFER_SIZE = 64 * 1024;

    private final AiYoloProperties properties;
    private final ObjectMapper objectMapper;
    private final Object cacheLock = new Object();
    private volatile CachedManifest cachedManifest;

    public BrowserInferenceModelService(AiYoloProperties properties) {
        this(properties, new ObjectMapper());
    }

    @Autowired
    public BrowserInferenceModelService(AiYoloProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void initializeManifest() {
        getCachedManifest();
    }

    public BrowserInferenceManifestResponse getManifest() {
        return getCachedManifest().manifest();
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
        if (cache != null
                && cache.path().equals(modelPath)
                && cache.fileSize() == size
                && cache.lastModified() == lastModified) {
            return cache;
        }
        synchronized (cacheLock) {
            cache = cachedManifest;
            if (cache != null
                    && cache.path().equals(modelPath)
                    && cache.fileSize() == size
                    && cache.lastModified() == lastModified) {
                return cache;
            }
            CachedManifest refreshed = loadManifest(modelPath, size, lastModified);
            cachedManifest = refreshed;
            return refreshed;
        }
    }

    private CachedManifest loadManifest(Path modelPath, long size, long lastModified) {
        try (InputStream inputStream = new ClassPathResource(MANIFEST_RESOURCE).getInputStream()) {
            BrowserInferenceManifestResponse manifest = objectMapper.readValue(inputStream, BrowserInferenceManifestResponse.class);
            if (!StringUtils.hasText(manifest.getSha256()) || manifest.getSha256().length() != 64) {
                throw new BusinessException(500, "Browser inference manifest sha256 is invalid");
            }
            if (manifest.getFileSize() != size) {
                throw new BusinessException(500, "Browser inference manifest fileSize does not match the model file");
            }
            String actualSha256 = sha256(modelPath);
            if (!manifest.getSha256().equalsIgnoreCase(actualSha256)) {
                throw new BusinessException(500, "Browser inference manifest sha256 does not match the model file");
            }
            manifest.setModelUrl("/api/ai/browser-inference/model?sha256=" + manifest.getSha256());
            return new CachedManifest(modelPath, size, lastModified, manifest.getSha256(), manifest);
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to load browser inference manifest: " + exception.getMessage());
        }
    }

    private Path resolveModelPath() {
        if (!StringUtils.hasText(properties.getModelPath())) {
            throw new BusinessException(500, "ai.yolo.model-path must not be blank");
        }
        Path modelPath = ProjectPathResolver.resolve(properties.getModelPath());
        if (!Files.isRegularFile(modelPath)) {
            throw new BusinessException(500, "ONNX model file does not exist: " + modelPath);
        }
        return modelPath;
    }

    private long fileSize(Path modelPath) {
        try {
            return Files.size(modelPath);
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to read ONNX model file size: " + exception.getMessage());
        }
    }

    private long lastModified(Path modelPath) {
        try {
            return Files.getLastModifiedTime(modelPath).toMillis();
        } catch (IOException exception) {
            throw new BusinessException(500, "Failed to read ONNX model file timestamp: " + exception.getMessage());
        }
    }

    private String sha256(Path modelPath) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = Files.newInputStream(modelPath)) {
                byte[] buffer = new byte[HASH_BUFFER_SIZE];
                int read;
                while ((read = inputStream.read(buffer)) >= 0) {
                    if (read > 0) {
                        digest.update(buffer, 0, read);
                    }
                }
            }
            StringBuilder result = new StringBuilder(64);
            for (byte value : digest.digest()) {
                result.append(String.format("%02x", value));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException | IOException exception) {
            throw new BusinessException(500, "Failed to calculate ONNX model sha256: " + exception.getMessage());
        }
    }

    private record CachedManifest(
            Path path,
            long fileSize,
            long lastModified,
            String sha256,
            BrowserInferenceManifestResponse manifest
    ) {
    }
}
