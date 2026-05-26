package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.util.List;

@Data
public class OrderMachineSummaryVo {

    private Long machineId;

    private String machineCode;

    private String machineName;

    private String machineType;

    private String status;

    private List<Long> relatedPlanIds;

    private List<Long> relatedPlanStepIds;

    private List<OrderMachineOccupiedRangeVo> occupiedTimeRanges;
}
