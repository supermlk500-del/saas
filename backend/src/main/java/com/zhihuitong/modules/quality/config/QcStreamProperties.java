package com.zhihuitong.modules.quality.config;

import jakarta.validation.constraints.Min;
import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

@Data
@Component
@Validated
@ConfigurationProperties(prefix = "qc.stream")
public class QcStreamProperties {

    @Min(value = 1, message = "qc.stream.frame-sample-interval-ms must be greater than 0")
    private long frameSampleIntervalMs = 250L;

    @Min(value = 1000, message = "qc.stream.session-idle-timeout-ms must be greater than or equal to 1000")
    private long sessionIdleTimeoutMs = 300000L;

    @Min(value = 1024, message = "qc.stream.max-frame-size-bytes must be greater than or equal to 1024")
    private int maxFrameSizeBytes = 5 * 1024 * 1024;

    @Min(value = 1000, message = "qc.stream.auto-save-defect-interval-ms must be greater than or equal to 1000")
    private long autoSaveDefectIntervalMs = 5000L;
}
