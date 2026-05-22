package com.zhihuitong.modules.process.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Data;

@Data
@EqualsAndHashCode(callSuper = true)
public class CapabilityQuery extends PageQuery {

    private Long stepId;

    private Long machineId;

    private Integer isActive;
}
