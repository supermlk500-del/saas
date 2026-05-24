package com.zhihuitong.modules.quality.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Data;

@Data
@EqualsAndHashCode(callSuper = true)
public class QcCameraQuery extends PageQuery {

    private String cameraCode;

    private String cameraName;

    private String cameraType;

    private Integer status;
}
