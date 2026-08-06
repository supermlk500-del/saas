package com.zhihuitong.modules.ai.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

/**
 * Browser inference model distribution settings. The backend serves and
 * validates the model artifact; it does not execute ONNX inference.
 */
@Data
@Component
@Validated
@ConfigurationProperties(prefix = "ai.yolo")
public class AiYoloProperties {

    private boolean enabled = true;

    @NotBlank(message = "ai.yolo.model-path must not be blank")
    private String modelPath = "C:/Users/lhr/Desktop/saas/docs/best.onnx";
}
