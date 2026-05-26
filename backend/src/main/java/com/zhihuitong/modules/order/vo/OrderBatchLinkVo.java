package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderBatchLinkVo {

    private Long id;

    private Long orderId;

    private Long orderItemId;

    private Long batchId;

    private String batchNo;

    private String supplier;

    private BigDecimal weight;

    private BigDecimal width;

    private String composition;

    private BigDecimal allocatedWeight;

    private BigDecimal allocatedQuantity;

    private BigDecimal remainingWeight;

    private BigDecimal remainingQuantity;

    private String status;

    private String resourceStatus;

    private String resourceStatusLabel;

    private Long currentPlanId;

    private String currentPlanStatus;

    private boolean lockedByPlan;

    private boolean readyForSchedule;

    private String remark;
}
