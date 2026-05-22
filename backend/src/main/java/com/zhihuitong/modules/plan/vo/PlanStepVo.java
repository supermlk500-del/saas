package com.zhihuitong.modules.plan.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PlanStepVo {

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

    private String remark;
}
