package com.zhihuitong.modules.plan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("productionplan")
public class ProductionPlan {

    @TableId(value = "planId", type = IdType.ASSIGN_ID)
    private Long planId;

    private Long orderId;

    private Long orderItemId;

    private Long batchId;

    private Long routeId;

    private LocalDateTime planStartTime;

    private LocalDateTime planEndTime;

    private String status;

    private Long deptId;

    private Long createdBy;

    private LocalDateTime createTime;

    private String remark;
}
