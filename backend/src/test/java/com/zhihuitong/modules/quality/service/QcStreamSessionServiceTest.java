package com.zhihuitong.modules.quality.service;

import com.zhihuitong.modules.ai.service.OnnxYoloService;
import com.zhihuitong.modules.plan.service.PlanStepService;
import com.zhihuitong.modules.quality.config.QcStreamProperties;
import com.zhihuitong.modules.quality.dto.ClientRealtimeEventRequest;
import com.zhihuitong.modules.quality.dto.QcStreamSessionCreateRequest;
import com.zhihuitong.modules.quality.vo.InspectionIntegrationResultVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QcStreamSessionServiceTest {

    private InspectionIntegrationService inspectionIntegrationService;
    private QcStreamSessionService service;

    @BeforeEach
    void setUp() {
        inspectionIntegrationService = mock(InspectionIntegrationService.class);
        service = new QcStreamSessionService(
                new QcStreamProperties(),
                mock(PlanStepService.class),
                mock(QcItemService.class),
                mock(QcCameraService.class),
                mock(OnnxYoloService.class),
                inspectionIntegrationService
        );
    }

    @Test
    void persistClientEvent_isIdempotentPerEventId() {
        String sessionId = createSession();
        ClientRealtimeEventRequest request = event("event-1");
        MockMultipartFile source = image("sourceFile");
        MockMultipartFile resultFile = image("resultFile");
        InspectionIntegrationResultVo saved = new InspectionIntegrationResultVo();
        saved.setInspectionId(101L);
        when(inspectionIntegrationService.persistClientStreamEvent(any(), any(), any(), any())).thenReturn(saved);

        InspectionIntegrationResultVo first = service.persistClientEvent(sessionId, request, source, resultFile);
        InspectionIntegrationResultVo duplicate = service.persistClientEvent(sessionId, request, source, resultFile);

        assertSame(saved, first);
        assertSame(saved, duplicate);
        verify(inspectionIntegrationService, times(1)).persistClientStreamEvent(any(), any(), any(), any());
    }

    @Test
    void persistClientEvent_allowsRetryAfterPersistenceFailure() {
        String sessionId = createSession();
        ClientRealtimeEventRequest request = event("event-retry");
        MockMultipartFile source = image("sourceFile");
        MockMultipartFile resultFile = image("resultFile");
        InspectionIntegrationResultVo saved = new InspectionIntegrationResultVo();
        saved.setInspectionId(102L);
        when(inspectionIntegrationService.persistClientStreamEvent(any(), any(), any(), any()))
                .thenThrow(new IllegalStateException("temporary failure"))
                .thenReturn(saved);

        assertThrows(
                IllegalStateException.class,
                () -> service.persistClientEvent(sessionId, request, source, resultFile)
        );
        InspectionIntegrationResultVo retried = service.persistClientEvent(sessionId, request, source, resultFile);

        assertSame(saved, retried);
        verify(inspectionIntegrationService, times(2)).persistClientStreamEvent(any(), any(), any(), any());
    }

    private String createSession() {
        QcStreamSessionCreateRequest request = new QcStreamSessionCreateRequest();
        request.setPlanStepId(1L);
        request.setQcItemId(1L);
        return service.createSession(request).getSessionId();
    }

    private ClientRealtimeEventRequest event(String eventId) {
        ClientRealtimeEventRequest request = new ClientRealtimeEventRequest();
        request.setEventId(eventId);
        request.setModelSha256("a".repeat(64));
        request.setImageWidth(10);
        request.setImageHeight(8);
        request.setProviderStrategy("WASM");
        return request;
    }

    private MockMultipartFile image(String name) {
        return new MockMultipartFile(name, name + ".png", "image/png", new byte[]{1});
    }
}
