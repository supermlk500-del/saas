package com.zhihuitong.modules.quality.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.ai.dto.BrowserInferenceClassItem;
import com.zhihuitong.modules.ai.service.BrowserInferenceModelService;
import com.zhihuitong.modules.quality.dto.ClientDetectionBox;
import com.zhihuitong.modules.quality.dto.ClientImageDetectionRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ClientInferenceResultValidator {

    private static final long MAX_STAGE_TIME_MS = 180_000;
    private static final long MAX_IMAGE_EDGE = 20_000;

    private final BrowserInferenceModelService browserInferenceModelService;

    public ClientInferenceResultValidator(BrowserInferenceModelService browserInferenceModelService) {
        this.browserInferenceModelService = browserInferenceModelService;
    }

    public Map<Integer, BrowserInferenceClassItem> validateImageResult(ClientImageDetectionRequest request,
                                                                       MultipartFile sourceFile,
                                                                       MultipartFile resultFile) {
        if (!browserInferenceModelService.getActiveSha256().equalsIgnoreCase(request.getModelSha256())) {
            throw new BusinessException(409, "Client result modelSha256 is not the active model");
        }
        validateImageMeta(request);
        ImageDimensions sourceDimensions = validateFile(sourceFile, "sourceFile");
        ImageDimensions resultDimensions = validateFile(resultFile, "resultFile");
        if (sourceDimensions.width() != request.getImageWidth()
                || sourceDimensions.height() != request.getImageHeight()) {
            throw new BusinessException(400, "sourceFile dimensions do not match payload image dimensions");
        }
        if (!sourceDimensions.equals(resultDimensions)) {
            throw new BusinessException(400, "sourceFile and resultFile dimensions must match");
        }
        Map<Integer, BrowserInferenceClassItem> classMap = browserInferenceModelService.getManifest().getClasses().stream()
                .collect(Collectors.toMap(BrowserInferenceClassItem::getIndex, Function.identity()));
        List<ClientDetectionBox> detections = request.getDetections();
        if (detections == null) {
            throw new BusinessException(400, "detections must not be null");
        }
        for (ClientDetectionBox box : detections) {
            if (box == null) {
                throw new BusinessException(400, "detections must not contain null items");
            }
            validateBox(box, request, classMap);
        }
        return classMap;
    }

    private void validateImageMeta(ClientImageDetectionRequest request) {
        if (request.getImageWidth() <= 0 || request.getImageHeight() <= 0
                || request.getImageWidth() > MAX_IMAGE_EDGE || request.getImageHeight() > MAX_IMAGE_EDGE) {
            throw new BusinessException(400, "Invalid image dimensions");
        }
        if (request.getPreprocessTimeMs() > MAX_STAGE_TIME_MS
                || request.getInferenceTimeMs() > MAX_STAGE_TIME_MS
                || request.getPostprocessTimeMs() > MAX_STAGE_TIME_MS) {
            throw new BusinessException(400, "Client inference stage time is out of range");
        }
        String provider = request.getProviderStrategy();
        if (!"WebGPU".equals(provider) && !"WASM".equals(provider)) {
            throw new BusinessException(400, "providerStrategy is not allowed");
        }
    }

    private ImageDimensions validateFile(MultipartFile file, String fieldName) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, fieldName + " must not be empty");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType) || !contentType.toLowerCase().startsWith("image/")) {
            throw new BusinessException(400, fieldName + " must be an image");
        }
        try (InputStream inputStream = file.getInputStream();
             ImageInputStream imageInputStream = ImageIO.createImageInputStream(inputStream)) {
            if (imageInputStream == null) {
                throw new BusinessException(400, fieldName + " is not a readable image");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new BusinessException(400, fieldName + " is not a supported image");
            }
            ImageReader reader = readers.next();
            try {
                reader.setInput(imageInputStream, true, true);
                int width = reader.getWidth(0);
                int height = reader.getHeight(0);
                if (width <= 0 || height <= 0 || width > MAX_IMAGE_EDGE || height > MAX_IMAGE_EDGE) {
                    throw new BusinessException(400, fieldName + " dimensions are out of range");
                }
                return new ImageDimensions(width, height);
            } finally {
                reader.dispose();
            }
        } catch (IOException exception) {
            throw new BusinessException(400, fieldName + " is not a readable image");
        }
    }

    private void validateBox(ClientDetectionBox box,
                             ClientImageDetectionRequest request,
                             Map<Integer, BrowserInferenceClassItem> classMap) {
        BrowserInferenceClassItem classItem = classMap.get(box.getClassIndex());
        if (classItem == null || !classItem.getCode().equals(box.getCode())) {
            throw new BusinessException(400, "Detection classIndex/code does not match active model classes");
        }
        if (!isFinite(box.getScore()) || !isFinite(box.getX1()) || !isFinite(box.getY1())
                || !isFinite(box.getX2()) || !isFinite(box.getY2())) {
            throw new BusinessException(400, "Detection contains non-finite number");
        }
        if (box.getScore() < 0 || box.getScore() > 1) {
            throw new BusinessException(400, "Detection score must be between 0 and 1");
        }
        if (box.getX2() <= box.getX1() || box.getY2() <= box.getY1()) {
            throw new BusinessException(400, "Detection box has invalid area");
        }
        if (box.getX1() < 0 || box.getY1() < 0
                || box.getX2() > request.getImageWidth()
                || box.getY2() > request.getImageHeight()) {
            throw new BusinessException(400, "Detection box is outside image bounds");
        }
    }

    private boolean isFinite(Number value) {
        return value != null && Double.isFinite(value.doubleValue());
    }

    private record ImageDimensions(int width, int height) {
    }
}
