package com.zhihuitong.modules.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("processroute")
public class ProcessRoute {

    @TableId(value = "routeId", type = IdType.ASSIGN_ID)
    private Long routeId;

    private String routeName;

    private String description;

    private Integer isActive;

    private LocalDateTime createTime;
}
