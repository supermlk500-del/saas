package com.zhihuitong.modules.plan.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class PlanMachineLoadItemVo {

    private Long machineId;

    private String machineName;

    private Integer stepCount;

    private BigDecimal totalPlanHours;

    private LocalDateTime firstStartTime;

    private LocalDateTime lastEndTime;
}
