package com.zhihuitong.modules.quality.vo;

import com.zhihuitong.modules.ai.model.YoloBox;
import com.zhihuitong.modules.ai.model.YoloDetectResult;
import com.zhihuitong.modules.quality.entity.InspectionData;
import com.zhihuitong.modules.quality.entity.QcRecord;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class InspectionIntegrationResultVo {

    private Long inspectionId;

    private String resultJudge;

    private BigDecimal confidenceScore;

    private String resultValue;

    private String imageUrl;

    private String sourceImageUrl;

    private List<YoloBox> boxes;

    private QcRecord qcRecord;

    private List<InspectionData> inspectionDataList;

    private YoloDetectResult algorithmResult;
}
