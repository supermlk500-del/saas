package com.zhihuitong.modules.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("orderbatchlink")
public class OrderBatchLink {

    @TableId(value = "id", type = IdType.ASSIGN_ID)
    private Long id;

    private Long orderId;

    private Long orderItemId;

    private Long batchId;

    private BigDecimal allocatedWeight;

    private BigDecimal allocatedQuantity;

    private String remark;
}
