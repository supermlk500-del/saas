package com.zhihuitong.modules.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("stepmachinecapability")
public class StepMachineCapability {

    @TableId(value = "capId", type = IdType.ASSIGN_ID)
    private Long capId;

    private Long stepId;

    private Long machineId;

    private BigDecimal minWidth;

    private BigDecimal maxWidth;

    private BigDecimal maxSpeed;

    private BigDecimal maxBatchWeight;

    private Integer isActive;
}
