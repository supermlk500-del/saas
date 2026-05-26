package com.zhihuitong.modules.batch.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class BatchResourcePoolVo {

    private Long batchId;

    private String batchNo;

    private String supplier;

    private LocalDateTime inDate;

    private BigDecimal weight;

    private BigDecimal width;

    private String composition;

    private String note;

    private String status;

    private String resourceStatus;

    private String resourceStatusLabel;

    private long linkedOrderCount;

    private List<BatchLinkedOrderVo> linkedOrders;

    private BigDecimal allocatedWeight;

    private BigDecimal allocatedQuantity;

    private BigDecimal remainingWeight;

    private BigDecimal remainingQuantity;

    private Long currentPlanId;

    private String currentPlanStatus;

    private Long currentOrderId;

    private String currentOrderNo;

    private boolean lockedByPlan;

    private boolean readyForSchedule;
}
