package com.zhihuitong.modules.plan.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlanConstraintQuery extends PageQuery {

    private String keyword;

    private String scope;

    private String constraintType;

    private String status;
}
