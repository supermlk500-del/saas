package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderPlanStepSummaryVo {

    private Long planStepId;

    private Long planId;

    private Long stepId;

    private String stepName;

    private Long machineId;

    private String machineName;

    private LocalDateTime planStartTime;

    private LocalDateTime planEndTime;

    private BigDecimal planHours;

    private Integer sequenceNo;

    private String status;
}
