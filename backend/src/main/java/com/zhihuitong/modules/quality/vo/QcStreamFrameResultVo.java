package com.zhihuitong.modules.quality.vo;

import com.zhihuitong.modules.ai.model.YoloBox;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class QcStreamFrameResultVo {

    private Long inspectionId;

    private String sessionId;

    private LocalDateTime frameTime;

    private String resultJudge;

    private BigDecimal confidenceScore;

    private String resultValue;

    private List<YoloBox> boxes;

    private String renderMode;

    private String imageUrl;

    private String sourceImageUrl;

    private Boolean autoSaved;
}
