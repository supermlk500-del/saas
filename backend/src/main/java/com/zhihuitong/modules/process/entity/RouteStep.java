package com.zhihuitong.modules.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("routestep")
public class RouteStep {

    @TableId(value = "routeStepId", type = IdType.ASSIGN_ID)
    private Long routeStepId;

    private Long routeId;

    private Long stepId;

    private Integer sortOrder;

    private Integer isMandatory;
}
