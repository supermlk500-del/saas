package com.zhihuitong.modules.system.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.system.dto.RoleStatusRequest;
import com.zhihuitong.modules.system.dto.SystemRoleQuery;
import com.zhihuitong.modules.system.dto.SystemRoleSaveRequest;
import com.zhihuitong.modules.system.entity.SysRole;
import com.zhihuitong.modules.system.service.SystemRoleService;
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

import java.util.LinkedHashMap;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/system/role")
public class SystemRoleController {
    private final SystemRoleService roleService;

    public SystemRoleController(SystemRoleService roleService) { this.roleService = roleService; }

    @PreAuthorize("@auth.hasPermission('system:role:list')")
    @GetMapping("/list")
    public TableDataInfo<SysRole> list(@Valid @ModelAttribute SystemRoleQuery query) { return roleService.list(query); }

    @PreAuthorize("@auth.hasPermission('system:role:list')")
    @GetMapping("/{roleId}")
    public AjaxResult detail(@PathVariable Long roleId) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("role", roleService.detail(roleId));
        result.put("menuIds", roleService.menuIds(roleId));
        result.put("deptIds", roleService.deptIds(roleId));
        return AjaxResult.success(result);
    }

    @PreAuthorize("@auth.hasPermission('system:role:manage')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody SystemRoleSaveRequest request) { return AjaxResult.success(roleService.create(request)); }

    @PreAuthorize("@auth.hasPermission('system:role:manage')")
    @PutMapping
    public AjaxResult update(@Valid @RequestBody SystemRoleSaveRequest request) { return AjaxResult.success(roleService.update(request)); }

    @PreAuthorize("@auth.hasPermission('system:role:manage')")
    @PutMapping("/changeStatus")
    public AjaxResult changeStatus(@Valid @RequestBody RoleStatusRequest request) { roleService.changeStatus(request); return AjaxResult.success(); }

    @PreAuthorize("@auth.hasPermission('system:role:manage')")
    @DeleteMapping("/{roleId}")
    public AjaxResult delete(@PathVariable Long roleId) { roleService.delete(roleId); return AjaxResult.success(); }
}
