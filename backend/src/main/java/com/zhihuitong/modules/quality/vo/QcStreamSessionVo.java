package com.zhihuitong.modules.quality.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QcStreamSessionVo {

    private String sessionId;

    private String streamMode;

    private Long cameraId;

    private LocalDateTime startedAt;
}
