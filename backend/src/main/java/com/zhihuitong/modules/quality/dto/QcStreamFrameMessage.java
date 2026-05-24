package com.zhihuitong.modules.quality.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class QcStreamFrameMessage {

    private String frameData;

    private LocalDateTime frameTime;
}
