package com.zhihuitong.modules.system.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SystemUserSaveRequest {
    private Long userId;
    @NotBlank private String userName;
    @NotBlank private String nickName;
    private String password;
    @Email private String email;
    private String phonenumber;
    private String avatar;
    @NotNull private Long deptId;
    @NotNull private Long postId;
    @NotNull private Long roleId;
    private String status;
    private String remark;
}
