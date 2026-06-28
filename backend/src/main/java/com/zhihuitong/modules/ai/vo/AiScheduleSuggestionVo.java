package com.zhihuitong.modules.ai.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiScheduleSuggestionVo {

    private Long recordId;
    private Long planId;
    private String orderNo;
    private String batchNo;
    private LocalDateTime deliveryDate;
    private String decisionEngine = "OR-Tools CP-SAT";
    private String explanationModel = "deepseek-v4-flash";
    private String notice = "排产结果由运筹优化算法计算，大模型仅用于解释；应用前必须人工确认。";
    private String recordStatus;
    private String selectedStrategy;
    private LocalDateTime createTime;
    private LocalDateTime appliedTime;
    private List<AiScheduleOptionVo> options = new ArrayList<>();
}
