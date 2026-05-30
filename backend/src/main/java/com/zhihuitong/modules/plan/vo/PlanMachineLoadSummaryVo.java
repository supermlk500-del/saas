package com.zhihuitong.modules.plan.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

@Data
public class PlanMachineLoadSummaryVo {

    private Long planId;

    private Integer totalStepCount;

    private Integer assignedStepCount;

    private Integer unassignedStepCount;

    private Integer machineCount;

    private BigDecimal totalPlanHours;

    private List<PlanMachineLoadItemVo> machines = Collections.emptyList();
}
