package com.zhihuitong.modules.exception.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("exceptionrecord")
public class ExceptionRecord {

    @TableId(value = "exceptionId", type = IdType.ASSIGN_ID)
    private Long exceptionId;

    private Long planStepId;

    private String exceptionType;

    private String exceptionLevel;

    private String description;

    private String handleResult;

    private LocalDateTime createTime;

    private String status;
}
