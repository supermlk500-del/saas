package com.zhihuitong.modules.system.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("sys_login_log")
public class SysLoginLog {
    @TableId(value = "infoId", type = IdType.ASSIGN_ID)
    private Long infoId;
    private Long userId;
    private String userName;
    private String sessionId;
    private String ipaddr;
    private String browser;
    private String os;
    private String status;
    private String message;
    private LocalDateTime loginTime;
}
