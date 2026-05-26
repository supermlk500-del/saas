package com.zhihuitong.modules.order.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class OrderItemVo {

    private Long orderItemId;

    private Long orderId;

    private String productCode;

    private String productName;

    private String specification;

    private String color;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal requiredWidth;

    private BigDecimal targetWidth;

    private BigDecimal requiredWeight;

    private BigDecimal targetWeight;

    private String remark;
}
