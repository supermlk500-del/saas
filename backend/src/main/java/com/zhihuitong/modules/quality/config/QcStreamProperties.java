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

    @Min(value = 1000, message = "qc.stream.session-idle-timeout-ms must be greater than or equal to 1000")
    private long sessionIdleTimeoutMs = 300000L;
}
