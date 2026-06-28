package com.zhihuitong.modules.ai.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.modules.ai.dto.ScheduleSuggestionApplyRequest;
import com.zhihuitong.modules.ai.service.AiQualityAnalysisService;
import com.zhihuitong.modules.ai.service.AiSchedulingService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/ai")
public class AiDecisionController {

    private final AiSchedulingService schedulingService;
    private final AiQualityAnalysisService qualityAnalysisService;

    public AiDecisionController(AiSchedulingService schedulingService,
                                AiQualityAnalysisService qualityAnalysisService) {
        this.schedulingService = schedulingService;
        this.qualityAnalysisService = qualityAnalysisService;
    }

    @PreAuthorize("@auth.hasPermission('plan:production:list')")
    @PostMapping("/schedule/plans/{planId}/suggestions")
    public AjaxResult scheduleSuggestions(@PathVariable @Min(1) Long planId) {
        return AjaxResult.success(schedulingService.suggest(planId));
    }

    @PreAuthorize("@auth.hasPermission('plan:production:list')")
    @GetMapping("/schedule/records")
    public AjaxResult scheduleRecords(@RequestParam(required = false) Long planId) {
        return AjaxResult.success(schedulingService.listRecords(planId));
    }

    @PreAuthorize("@auth.hasPermission('plan:production:list')")
    @GetMapping("/schedule/records/{recordId}")
    public AjaxResult scheduleRecord(@PathVariable @Min(1) Long recordId) {
        return AjaxResult.success(schedulingService.getRecord(recordId));
    }

    @PreAuthorize("@auth.hasPermission('plan:production:reschedule')")
    @PostMapping("/schedule/plans/{planId}/apply")
    public AjaxResult applyScheduleSuggestion(@PathVariable @Min(1) Long planId,
                                              @Valid @RequestBody ScheduleSuggestionApplyRequest request) {
        return AjaxResult.success(schedulingService.apply(planId, request));
    }

    @PreAuthorize("@auth.hasPermission('quality:realtime:view')")
    @PostMapping("/quality/records/{inspectionId}/analysis")
    public AjaxResult qualityAnalysis(@PathVariable @Min(1) Long inspectionId) {
        return AjaxResult.success(qualityAnalysisService.analyze(inspectionId));
    }
}
