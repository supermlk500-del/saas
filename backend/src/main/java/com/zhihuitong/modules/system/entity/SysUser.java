package com.zhihuitong.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_user")
public class SysUser {
    @TableId(value = "userId", type = IdType.ASSIGN_ID)
    private Long userId;
    private Long deptId;
    private Long postId;
    private Long roleId;
    private String userName;
    private String nickName;
    private String email;
    private String phonenumber;
    private String avatar;
    @JsonIgnore
    private String password;
    private String status;
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
    private String loginIp;
    private LocalDateTime loginDate;
    private LocalDateTime pwdUpdateTime;
    private Long createdBy;
    private LocalDateTime createTime;
    private Long updatedBy;
    private LocalDateTime updateTime;
    private String remark;
}
