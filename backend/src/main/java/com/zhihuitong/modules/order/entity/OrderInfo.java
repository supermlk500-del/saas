package com.zhihuitong.modules.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("orderinfo")
public class OrderInfo {

    @TableId(value = "orderId", type = IdType.ASSIGN_ID)
    private Long orderId;

    private String orderNo;

    private String customerName;

    private LocalDateTime orderDate;

    private LocalDateTime deliveryDate;

    private String priority;

    private String status;

    private Long deptId;

    private Long createdBy;

    private String remark;

    private LocalDateTime createTime;
}
