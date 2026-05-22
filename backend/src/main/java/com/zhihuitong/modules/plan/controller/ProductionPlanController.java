package com.zhihuitong.modules.plan.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.plan.dto.PlanRescheduleRequest;
import com.zhihuitong.modules.plan.dto.ProductionPlanCreateRequest;
import com.zhihuitong.modules.plan.dto.ProductionPlanQuery;
import com.zhihuitong.modules.plan.dto.ProductionPlanUpdateRequest;
import com.zhihuitong.modules.plan.dto.StatusPatchRequest;
import com.zhihuitong.modules.plan.service.ProductionPlanService;
import com.zhihuitong.modules.plan.vo.ProductionPlanDetailVo;
import com.zhihuitong.modules.plan.vo.ProductionPlanListVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/production-plans")
public class ProductionPlanController {

    private final ProductionPlanService productionPlanService;

    public ProductionPlanController(ProductionPlanService productionPlanService) {
        this.productionPlanService = productionPlanService;
    }

    @GetMapping
    public TableDataInfo<ProductionPlanListVo> list(@Valid @ModelAttribute ProductionPlanQuery query) {
        return productionPlanService.list(query);
    }

    @PostMapping
    public AjaxResult create(@Valid @RequestBody ProductionPlanCreateRequest request) {
        return AjaxResult.success(productionPlanService.create(request));
    }

    @GetMapping("/{planId}")
    public AjaxResult detail(@PathVariable @Min(value = 1, message = "planId must be greater than 0") Long planId) {
        ProductionPlanDetailVo detail = productionPlanService.getDetail(planId);
        return AjaxResult.success(detail);
    }

    @PutMapping("/{planId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "planId must be greater than 0") Long planId,
                             @Valid @RequestBody ProductionPlanUpdateRequest request) {
        return AjaxResult.success(productionPlanService.update(planId, request));
    }

    @PatchMapping("/{planId}/status")
    public AjaxResult patchStatus(@PathVariable @Min(value = 1, message = "planId must be greater than 0") Long planId,
                                  @Valid @RequestBody StatusPatchRequest request) {
        return AjaxResult.success(productionPlanService.patchStatus(planId, request));
    }

    @PostMapping("/{planId}/reschedule")
    public AjaxResult reschedule(@PathVariable @Min(value = 1, message = "planId must be greater than 0") Long planId,
                                 @Valid @RequestBody PlanRescheduleRequest request) {
        return AjaxResult.success(productionPlanService.reschedule(planId, request));
    }

    @GetMapping("/{planId}/gantt")
    public AjaxResult gantt(@PathVariable @Min(value = 1, message = "planId must be greater than 0") Long planId) {
        return AjaxResult.success(productionPlanService.getGantt(planId));
    }
}
