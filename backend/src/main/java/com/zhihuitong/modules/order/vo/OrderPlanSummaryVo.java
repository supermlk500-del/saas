package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderPlanSummaryVo {

    private Long planId;

    private Long orderId;

    private String orderNo;

    private String customerName;

    private Long orderItemId;

    private String productCode;

    private String productName;

    private String specification;

    private String color;

    private Long batchId;

    private String batchNo;

    private Long routeId;

    private String routeName;

    private String status;

    private LocalDateTime planStartTime;

    private LocalDateTime planEndTime;

    private String remark;

    private List<OrderPlanStepSummaryVo> planSteps;
}
