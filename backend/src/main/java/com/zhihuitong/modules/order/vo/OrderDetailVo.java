package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderDetailVo {

    private Long orderId;

    private String orderNo;

    private String customerName;

    private LocalDateTime orderDate;

    private LocalDateTime deliveryDate;

    private String priority;

    private String status;

    private String remark;

    private LocalDateTime createTime;

    private long linkedBatchCount;

    private long generatedPlanCount;

    private long qcRecordCount;

    private long exceptionCount;

    private List<OrderItemVo> items;

    private List<OrderBatchLinkVo> linkedBatches;

    private List<OrderPlanSummaryVo> planSummary;

    private List<OrderPlanStepSummaryVo> planSteps;

    private List<OrderRouteSummaryVo> routeSummary;

    private List<OrderMachineSummaryVo> machineSummary;

    private List<OrderQcSummaryVo> qcSummary;

    private List<OrderQcSummaryVo> qualitySummary;

    private OrderQcSummaryVo latestQcRecord;

    private List<OrderExceptionSummaryVo> exceptionSummary;

    private OrderExceptionSummaryVo latestException;
}
