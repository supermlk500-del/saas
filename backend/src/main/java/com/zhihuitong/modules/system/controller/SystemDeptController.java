package com.zhihuitong.modules.system.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.modules.system.entity.SysDept;
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
@RequestMapping("/system/dept")
public class SystemDeptController {
    private final SystemResourceService service;
    public SystemDeptController(SystemResourceService service) { this.service = service; }
    @PreAuthorize("@auth.hasPermission('system:dept:list')") @GetMapping("/list") public AjaxResult list(@RequestParam(required = false) String deptName, @RequestParam(required = false) String status) { return AjaxResult.success(service.listDepts(deptName, status)); }
    @PreAuthorize("@auth.hasPermission('system:dept:manage')") @PostMapping public AjaxResult create(@RequestBody SysDept request) { request.setDeptId(null); return AjaxResult.success(service.saveDept(request)); }
    @PreAuthorize("@auth.hasPermission('system:dept:manage')") @PutMapping public AjaxResult update(@RequestBody SysDept request) { return AjaxResult.success(service.saveDept(request)); }
    @PreAuthorize("@auth.hasPermission('system:dept:manage')") @DeleteMapping("/{deptId}") public AjaxResult delete(@PathVariable Long deptId) { service.deleteDept(deptId); return AjaxResult.success(); }
}
