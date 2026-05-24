package com.zhihuitong.modules.quality.config;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "inspection.storage")
public class InspectionStorageProperties {

    @NotBlank(message = "inspection.storage.upload-root must not be blank")
    private String uploadRoot = "C:/Users/lhr/Desktop/saas/photo/upload";

    @NotBlank(message = "inspection.storage.result-root must not be blank")
    private String resultRoot = "C:/Users/lhr/Desktop/saas/photo/results";
}
