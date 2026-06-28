package com.zhihuitong.modules.system.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.security.service.LoginSessionService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/online")
public class OnlineUserController {
    private final LoginSessionService sessionService;
    public OnlineUserController(LoginSessionService sessionService) { this.sessionService = sessionService; }
    @PreAuthorize("@auth.hasPermission('system:online:list')") @GetMapping public AjaxResult list() { return AjaxResult.success(sessionService.listOnlineSessions()); }
    @PreAuthorize("@auth.hasPermission('system:online:forceLogout')") @DeleteMapping("/{sessionId}") public AjaxResult forceLogout(@PathVariable String sessionId) { sessionService.delete(sessionId); return AjaxResult.success(); }
}
