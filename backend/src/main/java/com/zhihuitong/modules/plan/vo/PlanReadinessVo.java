package com.zhihuitong.modules.plan.vo;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class PlanReadinessVo {

    private Long planId;

    private Boolean ready;

    private Integer totalStepCount;

    private Integer blockerCount;

    private Integer warningCount;

    private List<PlanReadinessIssueVo> issues = Collections.emptyList();
}
