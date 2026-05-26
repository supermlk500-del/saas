package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class OrderListVo {

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
}
