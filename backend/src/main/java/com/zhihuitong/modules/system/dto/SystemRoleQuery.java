package com.zhihuitong.modules.system.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SystemRoleQuery extends PageQuery {
    private String roleName;
    private String roleKey;
    private String status;
}
