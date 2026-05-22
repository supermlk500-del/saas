package com.zhihuitong.modules.plan.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ProductionPlanListVo {

    private Long planId;

    private Long batchId;

    private String batchNo;

    private Long routeId;

    private String routeName;

    private LocalDateTime planStartTime;

    private LocalDateTime planEndTime;

    private String status;

    private LocalDateTime createTime;

    private String remark;
}
