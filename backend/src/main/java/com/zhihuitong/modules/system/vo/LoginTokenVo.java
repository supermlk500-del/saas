package com.zhihuitong.modules.system.vo;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginTokenVo {
    private String token;
    private long expiresIn;
}
