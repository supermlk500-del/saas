package com.zhihuitong.modules.plan.vo;

import lombok.Data;

import java.util.Collections;
import java.util.List;

@Data
public class PlanUnassignedReasonVo {

    private Long planId;

    private Integer totalIssueCount;

    private Integer unassignedCount;

    private Integer abnormalCount;

    private Integer missingTimeCount;

    private List<PlanUnassignedReasonItemVo> items = Collections.emptyList();
}
