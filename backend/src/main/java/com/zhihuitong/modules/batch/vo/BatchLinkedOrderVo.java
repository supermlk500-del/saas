package com.zhihuitong.modules.batch.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BatchLinkedOrderVo {

    private Long linkId;

    private Long orderId;

    private String orderNo;

    private String customerName;

    private String orderStatus;

    private Long orderItemId;

    private String productCode;

    private String productName;

    private String specification;

    private BigDecimal allocatedWeight;

    private BigDecimal allocatedQuantity;

    private String remark;
}
