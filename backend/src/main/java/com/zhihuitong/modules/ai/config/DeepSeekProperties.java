package com.zhihuitong.modules.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "ai.deepseek")
public class DeepSeekProperties {

    private boolean enabled = true;

    private String baseUrl = "https://api.deepseek.com";

    private String apiKey;

    private String model = "deepseek-v4-flash";

    private int timeoutSeconds = 30;

    private int maxTokens = 1200;
}
