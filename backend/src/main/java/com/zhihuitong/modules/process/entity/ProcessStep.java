package com.zhihuitong.modules.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;

@Data
@TableName("processstep")
public class ProcessStep {

    @TableId(value = "stepId", type = IdType.ASSIGN_ID)
    private Long stepId;

    private String stepCode;

    private String stepName;

    private String stepType;

    private Integer sortOrder;

    private BigDecimal defaultHours;

    private String description;

    private Integer isActive;
}
