package com.zhihuitong.modules.ai.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class AiScheduleStepVo {

    private Long planStepId;
    private Long stepId;
    private String stepName;
    private Long machineId;
    private String machineCode;
    private String machineName;
    private int recommendationScore;
    private String recommendationReason;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private BigDecimal predictedHours;
}
