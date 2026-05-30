package com.zhihuitong.modules.plan.vo;

import lombok.Data;

@Data
public class PlanConstraintVo {

    private String key;

    private String name;

    private String scope;

    private String constraintType;

    private Boolean hardConstraint;

    private Integer priority;

    private String rule;

    private String violationResult;

    private String suggestion;

    private String status;
}
