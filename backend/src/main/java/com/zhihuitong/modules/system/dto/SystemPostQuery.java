package com.zhihuitong.modules.system.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SystemPostQuery extends PageQuery {
    private String postCode;
    private String postName;
    private String status;
}
