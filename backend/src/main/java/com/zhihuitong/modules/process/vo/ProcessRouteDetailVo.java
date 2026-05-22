package com.zhihuitong.modules.process.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class ProcessRouteDetailVo {

    private Long routeId;

    private String routeName;

    private String description;

    private Integer isActive;

    private LocalDateTime createTime;

    private List<RouteStepVo> steps;
}
