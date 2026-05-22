package com.zhihuitong.modules.process.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.EqualsAndHashCode;
import lombok.Data;

@Data
@EqualsAndHashCode(callSuper = true)
public class MachineQuery extends PageQuery {

    private String machineCode;

    private String machineName;

    private String machineType;

    private String status;
}
