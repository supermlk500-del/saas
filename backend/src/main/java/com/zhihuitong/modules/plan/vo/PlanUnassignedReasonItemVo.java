package com.zhihuitong.modules.plan.vo;

import lombok.Data;

@Data
public class PlanUnassignedReasonItemVo {

    private Long planStepId;

    private Long stepId;

    private String stepName;

    private Integer sequenceNo;

    private String status;

    private Long machineId;

    private String reasonCode;

    private String reasonDesc;

    private String suggestion;

    private String remark;
}
