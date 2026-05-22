package com.zhihuitong.modules.plan.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("processparameter")
public class ProcessParameter {

    @TableId(value = "paramId", type = IdType.ASSIGN_ID)
    private Long paramId;

    private Long planStepId;

    private String paramName;

    private String paramValue;

    private String unit;

    private String paramType;

    private LocalDateTime recordTime;

    private String remark;
}
