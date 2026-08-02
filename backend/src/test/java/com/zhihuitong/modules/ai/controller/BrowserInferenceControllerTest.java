package com.zhihuitong.modules.ai.controller;

import com.zhihuitong.modules.ai.service.BrowserInferenceModelService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BrowserInferenceControllerTest {

    @Test
    void endpointsRequireRealtimeDetectionPermission() {
        PreAuthorize annotation = BrowserInferenceController.class.getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("@auth.hasPermission('quality:realtime:detect')");
    }

    @Test
    void modelResponsePublishesVersionedCacheAndLengthHeaders() {
        String sha256 = "a".repeat(64);
        byte[] modelBytes = new byte[]{1, 2, 3, 4};
        BrowserInferenceModelService modelService = mock(BrowserInferenceModelService.class);
        when(modelService.getModelResource(sha256)).thenReturn(new ByteArrayResource(modelBytes));
        when(modelService.getActiveSha256()).thenReturn(sha256);
        when(modelService.getModelFileSize()).thenReturn((long) modelBytes.length);

        var response = new BrowserInferenceController(modelService).model(sha256);

        assertThat(response.getHeaders().getETag()).isEqualTo("\"" + sha256 + "\"");
        assertThat(response.getHeaders().getContentLength()).isEqualTo(modelBytes.length);
        assertThat(response.getHeaders().getCacheControl()).isEqualTo("private, max-age=604800, immutable");
        assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
                .contains("best-" + sha256.substring(0, 12) + ".onnx");
    }
}
