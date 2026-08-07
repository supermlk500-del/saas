package com.zhihuitong.modules.ai.config;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.ProjectPathResolver;
import jakarta.annotation.PostConstruct;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Path;

@Component
public class AiYoloValidator {

    private final AiYoloProperties properties;

    public AiYoloValidator(AiYoloProperties properties) {
        this.properties = properties;
    }

    @PostConstruct
    public void validateModelPath() {
        if (!properties.isEnabled()) {
            return;
        }
        Path modelPath = ProjectPathResolver.resolve(properties.getModelPath());
        if (!Files.exists(modelPath) || !Files.isRegularFile(modelPath)) {
            throw new BusinessException(500, "YOLO ONNX model file does not exist: " + modelPath);
        }
    }
}
