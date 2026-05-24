package com.zhihuitong.modules.ai.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "ai.yolo")
public class AiYoloProperties {

    private boolean enabled = true;

    @NotBlank(message = "ai.yolo.model-path must not be blank")
    private String modelPath = "C:/Users/lhr/Desktop/saas/docs/best.onnx";

    @DecimalMin(value = "0.0", inclusive = true, message = "ai.yolo.conf-threshold must be between 0 and 1")
    @DecimalMax(value = "1.0", inclusive = true, message = "ai.yolo.conf-threshold must be between 0 and 1")
    private float confThreshold = 0.5F;

    @DecimalMin(value = "0.0", inclusive = true, message = "ai.yolo.iou-threshold must be between 0 and 1")
    @DecimalMax(value = "1.0", inclusive = true, message = "ai.yolo.iou-threshold must be between 0 and 1")
    private float iouThreshold = 0.45F;

    @DecimalMin(value = "0.0", inclusive = true, message = "ai.yolo.video-conf-threshold must be between 0 and 1")
    @DecimalMax(value = "1.0", inclusive = true, message = "ai.yolo.video-conf-threshold must be between 0 and 1")
    private float videoConfThreshold = 0.25F;

    @DecimalMin(value = "0.0", inclusive = true, message = "ai.yolo.video-fallback-conf-threshold must be between 0 and 1")
    @DecimalMax(value = "1.0", inclusive = true, message = "ai.yolo.video-fallback-conf-threshold must be between 0 and 1")
    private float videoFallbackConfThreshold = 0.12F;

    @DecimalMin(value = "0.0", inclusive = true, message = "ai.yolo.video-iou-threshold must be between 0 and 1")
    @DecimalMax(value = "1.0", inclusive = true, message = "ai.yolo.video-iou-threshold must be between 0 and 1")
    private float videoIouThreshold = 0.40F;
}
