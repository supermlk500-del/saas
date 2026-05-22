package com.zhihuitong.modules.process.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("machine")
public class Machine {

    @TableId(value = "machineId", type = IdType.ASSIGN_ID)
    private Long machineId;

    private String machineCode;

    private String machineName;

    private String machineType;

    private String description;

    private Integer status;

    private LocalDateTime createTime;
}
