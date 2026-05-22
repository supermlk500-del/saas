package com.zhihuitong.modules.exception.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Data;

@Data
@EqualsAndHashCode(callSuper = true)
public class ExceptionRecordQuery extends PageQuery {

    private Long planStepId;

    private String exceptionType;

    private String exceptionLevel;

    private String status;
}
