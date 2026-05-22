package com.zhihuitong.modules.quality.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Data;

@Data
@EqualsAndHashCode(callSuper = true)
public class InspectionDataQuery extends PageQuery {

    private Long qcRecordId;

    private Long cameraId;

    private String fileType;
}
