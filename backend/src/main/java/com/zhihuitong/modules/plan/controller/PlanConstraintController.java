package com.zhihuitong.modules.plan.controller;

import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.plan.dto.PlanConstraintQuery;
import com.zhihuitong.modules.plan.service.PlanConstraintService;
import com.zhihuitong.modules.plan.vo.PlanConstraintVo;
import jakarta.validation.Valid;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/plan-constraints")
public class PlanConstraintController {

    private final PlanConstraintService planConstraintService;

    public PlanConstraintController(PlanConstraintService planConstraintService) {
        this.planConstraintService = planConstraintService;
    }

    @GetMapping
    public TableDataInfo<PlanConstraintVo> list(@Valid @ModelAttribute PlanConstraintQuery query) {
        return planConstraintService.list(query);
    }
}
