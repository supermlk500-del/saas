package com.zhihuitong.modules.plan.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class PlanStepQuery extends PageQuery {

    private Long planId;

    private Long stepId;

    private Long machineId;

    private String status;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime dateTo;
}
