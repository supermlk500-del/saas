package com.zhihuitong.modules.plan.controller;

import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.plan.dto.PlanStrategyQuery;
import com.zhihuitong.modules.plan.service.PlanStrategyService;
import com.zhihuitong.modules.plan.vo.PlanStrategyVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/plan-strategies")
public class PlanStrategyController {

    private final PlanStrategyService planStrategyService;

    public PlanStrategyController(PlanStrategyService planStrategyService) {
        this.planStrategyService = planStrategyService;
    }

    @GetMapping
    public TableDataInfo<PlanStrategyVo> list(@Valid @ModelAttribute PlanStrategyQuery query) {
        return planStrategyService.list(query);
    }
}
