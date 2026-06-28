package com.zhihuitong.modules.system.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SystemRoleSaveRequest {
    private Long roleId;
    @NotBlank private String roleName;
    @NotBlank private String roleKey;
    @NotNull private Integer roleSort;
    @NotBlank private String dataScope;
    private String status;
    private String remark;
    private List<Long> menuIds;
    private List<Long> deptIds;
}
