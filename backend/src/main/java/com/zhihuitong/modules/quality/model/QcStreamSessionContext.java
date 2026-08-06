package com.zhihuitong.modules.quality.model;

import com.zhihuitong.modules.quality.vo.InspectionIntegrationResultVo;
import lombok.Data;

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

    /**
     * Session-local idempotency cache for retried evidence uploads.
     * The client event id is generated once per continuous hit.
     */
    private final Map<String, InspectionIntegrationResultVo> acceptedEvents = new ConcurrentHashMap<>();
}
