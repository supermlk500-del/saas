package com.zhihuitong.modules.system.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.modules.system.entity.SysMenu;
import com.zhihuitong.modules.system.service.SystemResourceService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/system/menu")
public class SystemMenuController {
    private final SystemResourceService service;
    public SystemMenuController(SystemResourceService service) { this.service = service; }

    @PreAuthorize("@auth.hasPermission('system:menu:list')")
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(required = false) String menuName, @RequestParam(required = false) String status) { return AjaxResult.success(service.listMenus(menuName, status)); }
    @PreAuthorize("@auth.hasPermission('system:menu:manage')") @PostMapping public AjaxResult create(@RequestBody SysMenu request) { request.setMenuId(null); return AjaxResult.success(service.saveMenu(request)); }
    @PreAuthorize("@auth.hasPermission('system:menu:manage')") @PutMapping public AjaxResult update(@RequestBody SysMenu request) { return AjaxResult.success(service.saveMenu(request)); }
    @PreAuthorize("@auth.hasPermission('system:menu:manage')") @DeleteMapping("/{menuId}") public AjaxResult delete(@PathVariable Long menuId) { service.deleteMenu(menuId); return AjaxResult.success(); }
}
