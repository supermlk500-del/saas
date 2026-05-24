package com.zhihuitong.modules.quality.service;

import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.ai.model.YoloDetectResult;
import com.zhihuitong.modules.ai.service.OnnxYoloService;
import com.zhihuitong.modules.plan.service.PlanStepService;
import com.zhihuitong.modules.quality.config.QcStreamProperties;
import com.zhihuitong.modules.quality.dto.QcStreamSessionCreateRequest;
import com.zhihuitong.modules.quality.dto.QcStreamSnapshotRequest;
import com.zhihuitong.modules.quality.model.QcStreamSessionContext;
import com.zhihuitong.modules.quality.vo.InspectionIntegrationResultVo;
import com.zhihuitong.modules.quality.vo.QcStreamFrameResultVo;
import com.zhihuitong.modules.quality.vo.QcStreamSessionVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class QcStreamSessionService {

    private static final Logger log = LoggerFactory.getLogger(QcStreamSessionService.class);

    private final Map<String, QcStreamSessionContext> sessions = new ConcurrentHashMap<>();

    private final QcStreamProperties qcStreamProperties;
    private final PlanStepService planStepService;
    private final QcItemService qcItemService;
    private final QcCameraService qcCameraService;
    private final OnnxYoloService onnxYoloService;
    private final InspectionIntegrationService inspectionIntegrationService;

    public QcStreamSessionService(QcStreamProperties qcStreamProperties,
                                  PlanStepService planStepService,
                                  QcItemService qcItemService,
                                  QcCameraService qcCameraService,
                                  OnnxYoloService onnxYoloService,
                                  InspectionIntegrationService inspectionIntegrationService) {
        this.qcStreamProperties = qcStreamProperties;
        this.planStepService = planStepService;
        this.qcItemService = qcItemService;
        this.qcCameraService = qcCameraService;
        this.onnxYoloService = onnxYoloService;
        this.inspectionIntegrationService = inspectionIntegrationService;
    }

    public QcStreamSessionVo createSession(QcStreamSessionCreateRequest request) {
        planStepService.requirePlanStep(request.getPlanStepId());
        qcItemService.requireQcItem(request.getQcItemId());
        qcCameraService.requireCamera(request.getCameraId());

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

    public void registerSocket(String sessionId, WebSocketSession socketSession) {
        QcStreamSessionContext context = requireSession(sessionId);
        context.getSockets().put(socketSession.getId(), socketSession);
        touch(context);
    }

    public void unregisterSocket(String sessionId, String socketId) {
        QcStreamSessionContext context = sessions.get(sessionId);
        if (context != null) {
            context.getSockets().remove(socketId);
            touch(context);
        }
    }

    public QcStreamFrameResultVo processFrame(String sessionId, byte[] frameBytes, LocalDateTime frameTime) {
        QcStreamSessionContext context = requireSession(sessionId);
        touch(context);
        if (frameBytes == null || frameBytes.length == 0) {
            throw new BusinessException(400, "Realtime frame must not be empty");
        }
        if (frameBytes.length > qcStreamProperties.getMaxFrameSizeBytes()) {
            throw new BusinessException(400, "Realtime frame exceeds max-frame-size-bytes limit");
        }

        LocalDateTime now = LocalDateTime.now();
        if (context.getLastProcessedAt() != null
                && java.time.Duration.between(context.getLastProcessedAt(), now).toMillis() < qcStreamProperties.getFrameSampleIntervalMs()) {
            QcStreamFrameResultVo latest = context.getLatestResult();
            if (latest != null) {
                latest.setRenderMode("reuse");
                latest.setFrameTime(frameTime != null ? frameTime : now);
                touch(context);
                return latest;
            }
        }

        YoloDetectResult detectResult = onnxYoloService.detectRealtime(frameBytes, "video");
        QcStreamFrameResultVo response = new QcStreamFrameResultVo();
        response.setSessionId(sessionId);
        response.setFrameTime(frameTime != null ? frameTime : now);
        response.setResultJudge(detectResult.getResultJudge());
        response.setConfidenceScore(detectResult.getConfidenceScore());
        response.setResultValue(detectResult.getResultValue());
        response.setBoxes(detectResult.getBoxes());
        response.setRenderMode("overlay");

        context.setLatestResult(response);
        context.setLastProcessedAt(now);
        touch(context);
        return response;
    }

    public InspectionIntegrationResultVo snapshot(String sessionId, QcStreamSnapshotRequest request) {
        QcStreamSessionContext context = requireSession(sessionId);
        MultipartFile file = request.getFile();
        if (file == null || file.isEmpty()) {
            throw new BusinessException(400, "snapshot file must not be empty");
        }
        touch(context);
        return inspectionIntegrationService.snapshotFromStreamSession(context, request);
    }

    public void closeSession(String sessionId) {
        QcStreamSessionContext context = sessions.remove(sessionId);
        if (context == null) {
            throw new BusinessException(404, "QC stream session not found");
        }
        closeSockets(context);
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
            long idleMillis = java.time.Duration.between(context.getLastActiveAt(), now).toMillis();
            if (idleMillis < qcStreamProperties.getSessionIdleTimeoutMs()) {
                return false;
            }
            log.info("Closing expired QC stream session: sessionId={}, idleMillis={}", context.getSessionId(), idleMillis);
            closeSockets(context);
            return true;
        });
    }

    public byte[] decodeBase64Frame(String frameData) {
        if (frameData == null || frameData.isBlank()) {
            throw new BusinessException(400, "frameData must not be blank");
        }
        String normalized = frameData;
        int commaIndex = normalized.indexOf(',');
        if (commaIndex >= 0) {
            normalized = normalized.substring(commaIndex + 1);
        }
        try {
            return Base64.getDecoder().decode(normalized);
        } catch (IllegalArgumentException exception) {
            throw new BusinessException(400, "frameData is not a valid base64 image");
        }
    }

    private void closeSockets(QcStreamSessionContext context) {
        context.getSockets().values().forEach(socket -> {
            try {
                if (socket.isOpen()) {
                    socket.close(CloseStatus.NORMAL);
                }
            } catch (IOException exception) {
                log.warn("Failed to close websocket session cleanly: sessionId={}, socketId={}, message={}",
                        context.getSessionId(), socket.getId(), exception.getMessage());
            }
        });
        context.getSockets().clear();
    }

    private void touch(QcStreamSessionContext context) {
        context.setLastActiveAt(LocalDateTime.now());
    }
}
