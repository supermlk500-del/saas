package com.zhihuitong.modules.plan.vo;

import lombok.Data;

@Data
public class PlanReadinessIssueVo {

    private String severity;

    private String code;

    private Long planStepId;

    private String message;

    private String suggestion;
}
