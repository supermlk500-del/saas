package com.zhihuitong.modules.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("inspectiondata")
public class InspectionData {

    @TableId(value = "dataId", type = IdType.ASSIGN_ID)
    private Long dataId;

    private Long qcRecordId;

    private Long cameraId;

    private String fileType;

    private String filePath;

    private String fileName;

    private LocalDateTime captureTime;

    private String resultSummary;

    private String remark;
}
