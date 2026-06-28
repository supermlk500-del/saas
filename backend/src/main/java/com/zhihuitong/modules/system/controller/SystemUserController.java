package com.zhihuitong.modules.system.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.system.dto.ResetPasswordRequest;
import com.zhihuitong.modules.system.dto.SystemUserQuery;
import com.zhihuitong.modules.system.dto.SystemUserSaveRequest;
import com.zhihuitong.modules.system.dto.UserStatusRequest;
import com.zhihuitong.modules.system.service.SystemUserService;
import com.zhihuitong.modules.system.vo.SystemUserVo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/system/user")
public class SystemUserController {
    private final SystemUserService userService;

    public SystemUserController(SystemUserService userService) { this.userService = userService; }

    @PreAuthorize("@auth.hasPermission('system:user:list')")
    @GetMapping("/list")
    public TableDataInfo<SystemUserVo> list(@Valid @ModelAttribute SystemUserQuery query) { return userService.list(query); }

    @PreAuthorize("@auth.hasPermission('system:user:list')")
    @GetMapping("/{userId}")
    public AjaxResult detail(@PathVariable Long userId) { return AjaxResult.success(userService.detail(userId)); }

    @PreAuthorize("@auth.hasPermission('system:user:manage')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody SystemUserSaveRequest request) { return AjaxResult.success(userService.create(request)); }

    @PreAuthorize("@auth.hasPermission('system:user:manage')")
    @PutMapping
    public AjaxResult update(@Valid @RequestBody SystemUserSaveRequest request) { return AjaxResult.success(userService.update(request)); }

    @PreAuthorize("@auth.hasPermission('system:user:manage')")
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@Valid @RequestBody UserStatusRequest request) { userService.changeStatus(request); return AjaxResult.success(); }

    @PreAuthorize("@auth.hasPermission('system:user:manage')")
    @PutMapping("/resetPwd")
    public AjaxResult resetPassword(@Valid @RequestBody ResetPasswordRequest request) { userService.resetPassword(request); return AjaxResult.success(); }

    @PreAuthorize("@auth.hasPermission('system:user:manage')")
    @DeleteMapping("/{userId}")
    public AjaxResult delete(@PathVariable Long userId) { userService.delete(userId); return AjaxResult.success(); }
}
