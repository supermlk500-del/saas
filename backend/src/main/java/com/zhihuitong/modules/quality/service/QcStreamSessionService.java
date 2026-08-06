package com.zhihuitong.modules.quality.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.plan.service.PlanStepService;
import com.zhihuitong.modules.quality.config.QcStreamProperties;
import com.zhihuitong.modules.quality.dto.ClientRealtimeEventRequest;
import com.zhihuitong.modules.quality.dto.QcStreamSessionCreateRequest;
import com.zhihuitong.modules.quality.model.QcStreamSessionContext;
import com.zhihuitong.modules.quality.vo.InspectionIntegrationResultVo;
import com.zhihuitong.modules.quality.vo.QcStreamSessionVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Owns the short-lived browser QC session and persists client-generated evidence.
 * Model inference deliberately stays in the browser worker to keep the realtime
 * loop independent from backend request latency.
 */
@Service
public class QcStreamSessionService {

    private static final Logger log = LoggerFactory.getLogger(QcStreamSessionService.class);

    private final Map<String, QcStreamSessionContext> sessions = new ConcurrentHashMap<>();

    private final QcStreamProperties qcStreamProperties;
    private final PlanStepService planStepService;
    private final QcItemService qcItemService;
    private final QcCameraService qcCameraService;
    private final InspectionIntegrationService inspectionIntegrationService;

    public QcStreamSessionService(QcStreamProperties qcStreamProperties,
                                  PlanStepService planStepService,
                                  QcItemService qcItemService,
                                  QcCameraService qcCameraService,
                                  InspectionIntegrationService inspectionIntegrationService) {
        this.qcStreamProperties = qcStreamProperties;
        this.planStepService = planStepService;
        this.qcItemService = qcItemService;
        this.qcCameraService = qcCameraService;
        this.inspectionIntegrationService = inspectionIntegrationService;
    }

    public QcStreamSessionVo createSession(QcStreamSessionCreateRequest request) {
        planStepService.requirePlanStep(request.getPlanStepId());
        qcItemService.requireQcItem(request.getQcItemId());
        if (request.getCameraId() != null) {
            qcCameraService.requireCamera(request.getCameraId());
        }

        String sessionId = UUID.randomUUID().toString().replace("-", "");
        LocalDateTime now = LocalDateTime.now();
        QcStreamSessionContext context = new QcStreamSessionContext();
        context.setSessionId(sessionId);
        context.setPlanStepId(request.getPlanStepId());
        context.setQcItemId(request.getQcItemId());
        context.setCameraId(request.getCameraId());
        context.setInspector(request.getInspector());
        context.setRemark(request.getRemark());
        context.setStartedAt(now);
        context.setLastActiveAt(now);
        sessions.put(sessionId, context);

        QcStreamSessionVo response = new QcStreamSessionVo();
        response.setSessionId(sessionId);
        response.setStreamMode("video");
        response.setCameraId(request.getCameraId());
        response.setStartedAt(now);
        return response;
    }

    public InspectionIntegrationResultVo persistClientEvent(String sessionId,
                                                            ClientRealtimeEventRequest request,
                                                            MultipartFile sourceFile,
                                                            MultipartFile resultFile) {
        QcStreamSessionContext context = requireSession(sessionId);
        touch(context);
        Map<String, InspectionIntegrationResultVo> acceptedEvents = context.getAcceptedEvents();
        synchronized (acceptedEvents) {
            InspectionIntegrationResultVo accepted = acceptedEvents.get(request.getEventId());
            if (accepted != null) {
                return accepted;
            }
            InspectionIntegrationResultVo result = inspectionIntegrationService.persistClientStreamEvent(
                    context,
                    request,
                    sourceFile,
                    resultFile
            );
            acceptedEvents.put(request.getEventId(), result);
            return result;
        }
    }

    public void closeSession(String sessionId) {
        if (sessions.remove(sessionId) == null) {
            throw new BusinessException(404, "QC stream session not found");
        }
    }

    public QcStreamSessionContext requireSession(String sessionId) {
        QcStreamSessionContext context = sessions.get(sessionId);
        if (context == null) {
            throw new BusinessException(404, "QC stream session not found");
        }
        return context;
    }

    @Scheduled(fixedDelay = 60000)
    public void cleanupExpiredSessions() {
        LocalDateTime now = LocalDateTime.now();
        sessions.entrySet().removeIf(entry -> {
            QcStreamSessionContext context = entry.getValue();
            long idleMillis = Duration.between(context.getLastActiveAt(), now).toMillis();
            if (idleMillis < qcStreamProperties.getSessionIdleTimeoutMs()) {
                return false;
            }
            log.info("Closing expired QC stream session: sessionId={}, idleMillis={}", context.getSessionId(), idleMillis);
            return true;
        });
    }

    private void touch(QcStreamSessionContext context) {
        context.setLastActiveAt(LocalDateTime.now());
    }
}
