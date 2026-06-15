package com.zhihuitong.modules.quality.model;

import com.zhihuitong.modules.quality.vo.QcStreamFrameResultVo;
import lombok.Data;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Data
public class QcStreamSessionContext {

    private String sessionId;

    private Long planStepId;

    private Long qcItemId;

    private Long cameraId;

    private String inspector;

    private String remark;

    private LocalDateTime startedAt;

    private volatile LocalDateTime lastActiveAt;

    private volatile LocalDateTime lastProcessedAt;

    private volatile LocalDateTime lastAutoSavedAt;

    private volatile QcStreamFrameResultVo latestResult;

    private final Map<String, WebSocketSession> sockets = new ConcurrentHashMap<>();
}
