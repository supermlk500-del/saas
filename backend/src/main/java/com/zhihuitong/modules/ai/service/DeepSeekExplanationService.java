package com.zhihuitong.modules.ai.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuitong.modules.ai.config.DeepSeekProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekExplanationService {

    private static final Logger log = LoggerFactory.getLogger(DeepSeekExplanationService.class);

    private final DeepSeekProperties properties;
    private final ObjectMapper objectMapper;
    private final RestClient restClient;

    public DeepSeekExplanationService(DeepSeekProperties properties,
                                      ObjectMapper objectMapper,
                                      RestClient.Builder builder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeoutSeconds() * 1000);
        requestFactory.setReadTimeout(properties.getTimeoutSeconds() * 1000);
        this.restClient = builder
                .baseUrl(properties.getBaseUrl())
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .requestFactory(requestFactory)
                .build();
    }

    public ExplanationResult explain(String systemPrompt, Object structuredFacts, String fallbackText) {
        if (!properties.isEnabled() || !StringUtils.hasText(properties.getApiKey())) {
            return new ExplanationResult(fallbackText, properties.getModel(), false, "API_KEY_NOT_CONFIGURED");
        }
        try {
            String factsJson = objectMapper.writeValueAsString(structuredFacts);
            Map<String, Object> payload = Map.of(
                    "model", properties.getModel(),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", factsJson)
                    ),
                    "thinking", Map.of("type", "disabled"),
                    "temperature", 0.2,
                    "max_tokens", properties.getMaxTokens(),
                    "stream", false
            );
            JsonNode response = restClient.post()
                    .uri("/chat/completions")
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + properties.getApiKey())
                    .body(payload)
                    .retrieve()
                    .body(JsonNode.class);
            String content = response == null ? null : response.path("choices").path(0).path("message").path("content").asText();
            if (!StringUtils.hasText(content)) {
                return new ExplanationResult(fallbackText, properties.getModel(), false, "EMPTY_MODEL_RESPONSE");
            }
            return new ExplanationResult(content.trim(), properties.getModel(), true, null);
        } catch (Exception exception) {
            log.warn("DeepSeek explanation failed, deterministic fallback will be used: type={}, message={}",
                    exception.getClass().getSimpleName(), exception.getMessage());
            return new ExplanationResult(fallbackText, properties.getModel(), false, "MODEL_CALL_FAILED");
        }
    }

    public record ExplanationResult(String content, String model, boolean aiGenerated, String fallbackReason) {
    }
}
