package com.zhihuitong.modules.system.dto;

import com.zhihuitong.common.model.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SystemUserQuery extends PageQuery {
    private String userName;
    private String phonenumber;
    private String status;
    private Long deptId;
}
