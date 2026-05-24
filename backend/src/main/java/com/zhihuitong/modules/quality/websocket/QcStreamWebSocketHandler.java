package com.zhihuitong.modules.quality.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.modules.quality.dto.QcStreamFrameMessage;
import com.zhihuitong.modules.quality.service.QcStreamSessionService;
import com.zhihuitong.modules.quality.vo.QcStreamFrameResultVo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.BinaryMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

@Component
public class QcStreamWebSocketHandler extends AbstractWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(QcStreamWebSocketHandler.class);

    private final QcStreamSessionService qcStreamSessionService;
    private final ObjectMapper objectMapper;

    public QcStreamWebSocketHandler(QcStreamSessionService qcStreamSessionService,
                                    ObjectMapper objectMapper) {
        this.qcStreamSessionService = qcStreamSessionService;
        this.objectMapper = objectMapper;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        String sessionId = extractSessionId(session);
        qcStreamSessionService.registerSocket(sessionId, session);
        log.info("QC stream websocket connected: sessionId={}, socketId={}", sessionId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        QcStreamFrameMessage frameMessage = objectMapper.readValue(message.getPayload(), QcStreamFrameMessage.class);
        byte[] frameBytes = qcStreamSessionService.decodeBase64Frame(frameMessage.getFrameData());
        QcStreamFrameResultVo result = qcStreamSessionService.processFrame(sessionId, frameBytes, frameMessage.getFrameTime());
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));
    }

    @Override
    protected void handleBinaryMessage(WebSocketSession session, BinaryMessage message) throws Exception {
        String sessionId = extractSessionId(session);
        ByteBuffer payload = message.getPayload();
        byte[] frameBytes = new byte[payload.remaining()];
        payload.get(frameBytes);
        QcStreamFrameResultVo result = qcStreamSessionService.processFrame(sessionId, frameBytes, null);
        session.sendMessage(new TextMessage(objectMapper.writeValueAsString(result)));
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        log.warn("QC stream websocket transport error: socketId={}, message={}", session.getId(), exception.getMessage());
        if (session.isOpen()) {
            session.close(CloseStatus.SERVER_ERROR);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        String sessionId = extractSessionId(session);
        qcStreamSessionService.unregisterSocket(sessionId, session.getId());
        log.info("QC stream websocket closed: sessionId={}, socketId={}, status={}", sessionId, session.getId(), status);
    }

    private String extractSessionId(WebSocketSession session) {
        if (session.getUri() == null) {
            throw new BusinessException(400, "WebSocket URI is missing");
        }
        List<String> segments = java.util.Arrays.stream(session.getUri().getPath().split("/"))
                .filter(segment -> !segment.isBlank())
                .toList();
        if (segments.isEmpty()) {
            throw new BusinessException(400, "QC stream sessionId is missing in websocket path");
        }
        return segments.get(segments.size() - 1);
    }
}
