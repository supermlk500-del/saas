package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.util.List;

@Data
public class OrderRouteSummaryVo {

    private Long routeId;

    private String routeName;

    private String description;

    private List<OrderRouteStepVo> steps;
}
