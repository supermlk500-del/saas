package com.zhihuitong.modules.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("qcrecord")
public class QcRecord {

    @TableId(value = "inspectionId", type = IdType.ASSIGN_ID)
    private Long inspectionId;

    private Long planStepId;

    private Long qcItemId;

    private LocalDateTime inspectTime;

    private String inspectType;

    private Long cameraId;

    private LocalDateTime frameTime;

    private String imageUrl;

    private BigDecimal confidenceScore;

    private String resultValue;

    private String resultJudge;

    private String inspector;

    private String remark;
}
