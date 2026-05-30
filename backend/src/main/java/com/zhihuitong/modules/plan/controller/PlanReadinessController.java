package com.zhihuitong.modules.plan.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.modules.plan.service.PlanReadinessService;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/plan-readiness")
public class PlanReadinessController {

    private final PlanReadinessService planReadinessService;

    public PlanReadinessController(PlanReadinessService planReadinessService) {
        this.planReadinessService = planReadinessService;
    }

    @GetMapping("/{planId}")
    public AjaxResult check(@PathVariable @Min(value = 1, message = "planId 必须大于 0") Long planId) {
        return AjaxResult.success(planReadinessService.check(planId));
    }
}
