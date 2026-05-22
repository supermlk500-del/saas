package com.zhihuitong.modules.plan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("planstep")
public class PlanStep {

    @TableId(value = "planStepId", type = IdType.ASSIGN_ID)
    private Long planStepId;

    private Long planId;

    private Long stepId;

    private Long machineId;

    private LocalDateTime planStartTime;

    private LocalDateTime planEndTime;

    private BigDecimal planHours;

    private Integer sequenceNo;

    private String status;

    private String remark;
}
