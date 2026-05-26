package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderSchedulePoolVo {

    private Long orderId;

    private String orderNo;

    private String customerName;

    private LocalDateTime orderDate;

    private Long orderItemId;

    private String productCode;

    private String productName;

    private String specification;

    private String color;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal requiredWidth;

    private BigDecimal requiredWeight;

    private LocalDateTime deliveryDate;

    private String priority;

    private String remark;

    private long linkedBatchCount;

    private long readyBatchCount;

    private long generatedPlanCount;

    private long activePlanCount;

    private String batchSummary;

    private BigDecimal allocatedWeight;

    private BigDecimal allocatedQuantity;

    private List<OrderBatchLinkVo> linkedBatches;

    private Long recommendedRouteId;

    private String recommendedRouteName;

    private OrderRouteSummaryVo recommendedRoute;

    private List<String> recommendedMachines;

    private List<OrderSchedulePoolMachineVo> recommendedMachineList;

    private String status;

    private String scheduleBlockedReason;

    private boolean readyForSchedule;
}
