package com.zhihuitong.modules.dashboard.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.modules.dashboard.service.DashboardService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {
    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @PreAuthorize("@auth.hasPermission('dashboard:view')")
    @GetMapping("/overview")
    public AjaxResult overview() {
        return AjaxResult.success(dashboardService.overview());
    }
}
