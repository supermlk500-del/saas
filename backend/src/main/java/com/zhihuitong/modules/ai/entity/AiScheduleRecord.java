package com.zhihuitong.modules.ai.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("aischedulerecord")
public class AiScheduleRecord {

    @TableId(value = "recordId", type = IdType.ASSIGN_ID)
    private Long recordId;

    private Long planId;

    private String status;

    private String selectedStrategy;

    private String snapshotJson;

    private Long deptId;

    private Long createdBy;

    private LocalDateTime createTime;

    private LocalDateTime appliedTime;
}
