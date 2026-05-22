package com.zhihuitong.modules.plan.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessParameterQuery extends PageQuery {

    private Long planStepId;

    private String paramType;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTimeFrom;

    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime recordTimeTo;
}
