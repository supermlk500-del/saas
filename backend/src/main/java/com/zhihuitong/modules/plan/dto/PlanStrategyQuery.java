package com.zhihuitong.modules.plan.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlanStrategyQuery extends PageQuery {

    private String keyword;

    private String status;
}
