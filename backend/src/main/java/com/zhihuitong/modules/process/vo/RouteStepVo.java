package com.zhihuitong.modules.process.vo;

import lombok.Data;

@Data
public class RouteStepVo {

    private Long routeStepId;

    private Long routeId;

    private Long stepId;

    private Integer sortOrder;

    private Integer isMandatory;

    private String stepCode;

    private String stepName;
}
