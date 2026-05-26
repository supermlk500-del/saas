package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderMachineOccupiedRangeVo {

    private Long planId;

    private Long planStepId;

    private String stepName;

    private LocalDateTime planStartTime;

    private LocalDateTime planEndTime;
}
