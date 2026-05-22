package com.zhihuitong.modules.process.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Data;

@Data
@EqualsAndHashCode(callSuper = true)
public class ProcessStepQuery extends PageQuery {

    private String stepCode;

    private String stepName;

    private String stepType;

    private Integer isActive;
}
