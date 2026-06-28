package com.zhihuitong.modules.system.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SystemUserVo {
    private Long userId;
    private Long deptId;
    private String deptName;
    private Long postId;
    private String postName;
    private Long roleId;
    private String roleName;
    private String roleKey;
    private String userName;
    private String nickName;
    private String email;
    private String phonenumber;
    private String avatar;
    private String status;
    private String loginIp;
    private LocalDateTime loginDate;
    private LocalDateTime createTime;
    private String remark;
}
