package com.zhihuitong.modules.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("orderitem")
public class OrderItem {

    @TableId(value = "orderItemId", type = IdType.ASSIGN_ID)
    private Long orderItemId;

    private Long orderId;

    private String productCode;

    private String productName;

    private String specification;

    private String color;

    private BigDecimal quantity;

    private String unit;

    private BigDecimal requiredWidth;

    private BigDecimal requiredWeight;

    private String remark;
}
