package com.zhihuitong.modules.process.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.process.dto.ProcessRouteQuery;
import com.zhihuitong.modules.process.dto.ProcessRouteUpsertRequest;
import com.zhihuitong.modules.process.dto.RouteStepUpsertRequest;
import com.zhihuitong.modules.process.entity.ProcessRoute;
import com.zhihuitong.modules.process.service.ProcessRouteService;
import com.zhihuitong.modules.process.vo.ProcessRouteDetailVo;
import com.zhihuitong.modules.process.vo.RouteStepVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
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

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/process-routes")
public class ProcessRouteController {

    private final ProcessRouteService processRouteService;

    public ProcessRouteController(ProcessRouteService processRouteService) {
        this.processRouteService = processRouteService;
    }

    @GetMapping
    public TableDataInfo<ProcessRoute> list(@Valid @ModelAttribute ProcessRouteQuery query) {
        return processRouteService.list(query);
    }

    @GetMapping("/{routeId}")
    public AjaxResult detail(@PathVariable @Min(value = 1, message = "routeId must be greater than 0") Long routeId) {
        ProcessRouteDetailVo detail = processRouteService.getDetail(routeId);
        return AjaxResult.success(detail);
    }

    @PostMapping
    public AjaxResult create(@Valid @RequestBody ProcessRouteUpsertRequest request) {
        return AjaxResult.success(processRouteService.create(request));
    }

    @PutMapping("/{routeId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "routeId must be greater than 0") Long routeId,
                             @Valid @RequestBody ProcessRouteUpsertRequest request) {
        return AjaxResult.success(processRouteService.update(routeId, request));
    }

    @DeleteMapping("/{routeId}")
    public AjaxResult delete(@PathVariable @Min(value = 1, message = "routeId must be greater than 0") Long routeId) {
        processRouteService.delete(routeId);
        return AjaxResult.success();
    }

    @GetMapping("/{routeId}/steps")
    public AjaxResult listRouteSteps(@PathVariable @Min(value = 1, message = "routeId must be greater than 0") Long routeId) {
        List<RouteStepVo> rows = processRouteService.listRouteSteps(routeId);
        return AjaxResult.success(rows);
    }

    @PostMapping("/{routeId}/steps")
    public AjaxResult createRouteStep(@PathVariable @Min(value = 1, message = "routeId must be greater than 0") Long routeId,
                                      @Valid @RequestBody RouteStepUpsertRequest request) {
        return AjaxResult.success(processRouteService.createRouteStep(routeId, request));
    }

    @PutMapping("/{routeId}/steps/{routeStepId}")
    public AjaxResult updateRouteStep(@PathVariable @Min(value = 1, message = "routeId must be greater than 0") Long routeId,
                                      @PathVariable @Min(value = 1, message = "routeStepId must be greater than 0") Long routeStepId,
                                      @Valid @RequestBody RouteStepUpsertRequest request) {
        return AjaxResult.success(processRouteService.updateRouteStep(routeId, routeStepId, request));
    }

    @DeleteMapping("/{routeId}/steps/{routeStepId}")
    public AjaxResult deleteRouteStep(@PathVariable @Min(value = 1, message = "routeId must be greater than 0") Long routeId,
                                      @PathVariable @Min(value = 1, message = "routeStepId must be greater than 0") Long routeStepId) {
        processRouteService.deleteRouteStep(routeId, routeStepId);
        return AjaxResult.success();
    }
}
