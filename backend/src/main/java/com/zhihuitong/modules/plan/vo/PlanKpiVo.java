package com.zhihuitong.modules.plan.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PlanKpiVo {

    private Long planId;

    private Integer totalStepCount;

    private Integer assignedStepCount;

    private Integer unassignedStepCount;

    private BigDecimal machineAssignmentRate;

    private BigDecimal totalPlanHours;

    private Integer onTimeStepCount;

    private Integer lateStepCount;

    private BigDecimal onTimeRate;

    private BigDecimal maxLateHours;

    private LocalDateTime plannedEndTime;

    private LocalDateTime deliveryDate;

    private Boolean deliveryDateMet;

    private Integer algorithmRemarkCount;

    private BigDecimal algorithmRemarkCoverageRate;
}
