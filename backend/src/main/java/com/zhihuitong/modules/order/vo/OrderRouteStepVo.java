package com.zhihuitong.modules.order.vo;

import lombok.Data;

@Data
public class OrderRouteStepVo {

    private Long routeStepId;

    private Long stepId;

    private String stepCode;

    private String stepName;

    private Integer sortOrder;

    private Integer isMandatory;
}
