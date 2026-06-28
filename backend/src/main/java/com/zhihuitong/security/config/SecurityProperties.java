package com.zhihuitong.security.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "security")
public class SecurityProperties {
    private Jwt jwt = new Jwt();
    private Captcha captcha = new Captcha();

    @Data
    public static class Jwt {
        private String secret;
        private long expireMinutes = 30;
        private long refreshThresholdMinutes = 10;
    }

    @Data
    public static class Captcha {
        private boolean enabled = true;
        private long expireMinutes = 5;
        private int length = 4;
    }
}
