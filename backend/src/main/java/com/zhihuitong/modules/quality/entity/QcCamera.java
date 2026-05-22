package com.zhihuitong.modules.quality.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("qccamera")
public class QcCamera {

    @TableId(value = "cameraId", type = IdType.ASSIGN_ID)
    private Long cameraId;

    private String cameraCode;

    private String cameraName;

    private String cameraType;

    private String ipAddress;

    private String location;

    private Integer status;

    private String remark;
}
