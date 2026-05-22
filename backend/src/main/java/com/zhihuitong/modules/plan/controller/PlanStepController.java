package com.zhihuitong.modules.plan.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.plan.dto.PlanStepMachinePatchRequest;
import com.zhihuitong.modules.plan.dto.PlanStepQuery;
import com.zhihuitong.modules.plan.dto.PlanStepUpdateRequest;
import com.zhihuitong.modules.plan.dto.StatusPatchRequest;
import com.zhihuitong.modules.plan.service.PlanStepService;
import com.zhihuitong.modules.plan.vo.PlanStepVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/plan-steps")
public class PlanStepController {

    private final PlanStepService planStepService;

    public PlanStepController(PlanStepService planStepService) {
        this.planStepService = planStepService;
    }

    @GetMapping
    public TableDataInfo<PlanStepVo> list(@Valid @ModelAttribute PlanStepQuery query) {
        return planStepService.list(query);
    }

    @GetMapping("/{planStepId}")
    public AjaxResult detail(@PathVariable @Min(value = 1, message = "planStepId must be greater than 0") Long planStepId) {
        return AjaxResult.success(planStepService.getDetail(planStepId));
    }

    @PutMapping("/{planStepId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "planStepId must be greater than 0") Long planStepId,
                             @Valid @RequestBody PlanStepUpdateRequest request) {
        return AjaxResult.success(planStepService.update(planStepId, request));
    }

    @PatchMapping("/{planStepId}/machine")
    public AjaxResult patchMachine(@PathVariable @Min(value = 1, message = "planStepId must be greater than 0") Long planStepId,
                                   @Valid @RequestBody PlanStepMachinePatchRequest request) {
        return AjaxResult.success(planStepService.patchMachine(planStepId, request));
    }

    @PatchMapping("/{planStepId}/status")
    public AjaxResult patchStatus(@PathVariable @Min(value = 1, message = "planStepId must be greater than 0") Long planStepId,
                                  @Valid @RequestBody StatusPatchRequest request) {
        return AjaxResult.success(planStepService.patchStatus(planStepId, request));
    }
}
