package com.zhihuitong.modules.process.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.plan.entity.ProductionPlan;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import com.zhihuitong.modules.process.dto.ProcessRouteQuery;
import com.zhihuitong.modules.process.dto.ProcessRouteUpsertRequest;
import com.zhihuitong.modules.process.dto.RouteStepUpsertRequest;
import com.zhihuitong.modules.process.entity.ProcessRoute;
import com.zhihuitong.modules.process.entity.ProcessStep;
import com.zhihuitong.modules.process.entity.RouteStep;
import com.zhihuitong.modules.process.mapper.ProcessRouteMapper;
import com.zhihuitong.modules.process.mapper.ProcessStepMapper;
import com.zhihuitong.modules.process.mapper.RouteStepMapper;
import com.zhihuitong.modules.process.vo.ProcessRouteDetailVo;
import com.zhihuitong.modules.process.vo.RouteStepVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class ProcessRouteService {

    private final ProcessRouteMapper processRouteMapper;
    private final RouteStepMapper routeStepMapper;
    private final ProcessStepMapper processStepMapper;
    private final ProductionPlanMapper productionPlanMapper;

    public ProcessRouteService(ProcessRouteMapper processRouteMapper,
                               RouteStepMapper routeStepMapper,
                               ProcessStepMapper processStepMapper,
                               ProductionPlanMapper productionPlanMapper) {
        this.processRouteMapper = processRouteMapper;
        this.routeStepMapper = routeStepMapper;
        this.processStepMapper = processStepMapper;
        this.productionPlanMapper = productionPlanMapper;
    }

    public TableDataInfo<ProcessRoute> list(ProcessRouteQuery query) {
        Page<ProcessRoute> page = processRouteMapper.selectPage(query.toPage(), Wrappers.<ProcessRoute>lambdaQuery()
                .like(StringUtils.hasText(query.getRouteName()), ProcessRoute::getRouteName, query.getRouteName())
                .eq(query.getIsActive() != null, ProcessRoute::getIsActive, query.getIsActive())
                .orderByDesc(ProcessRoute::getCreateTime));
        return TableDataInfoBuilder.build(page);
    }

    public ProcessRouteDetailVo getDetail(Long routeId) {
        ProcessRoute route = requireRoute(routeId);
        ProcessRouteDetailVo detail = new ProcessRouteDetailVo();
        detail.setRouteId(route.getRouteId());
        detail.setRouteName(route.getRouteName());
        detail.setDescription(route.getDescription());
        detail.setIsActive(route.getIsActive());
        detail.setCreateTime(route.getCreateTime());
        detail.setSteps(listRouteSteps(routeId));
        return detail;
    }

    @Transactional
    public ProcessRoute create(ProcessRouteUpsertRequest request) {
        ProcessRoute route = new ProcessRoute();
        route.setRouteName(request.getRouteName().trim());
        route.setDescription(request.getDescription());
        route.setIsActive(request.getIsActive());
        route.setCreateTime(LocalDateTime.now());
        processRouteMapper.insert(route);
        return route;
    }

    @Transactional
    public ProcessRoute update(Long routeId, ProcessRouteUpsertRequest request) {
        ProcessRoute route = requireRoute(routeId);
        route.setRouteName(request.getRouteName().trim());
        route.setDescription(request.getDescription());
        route.setIsActive(request.getIsActive());
        processRouteMapper.updateById(route);
        return route;
    }

    @Transactional
    public void delete(Long routeId) {
        requireRoute(routeId);
        Long count = productionPlanMapper.selectCount(Wrappers.<ProductionPlan>lambdaQuery()
                .eq(ProductionPlan::getRouteId, routeId));
        if (count != null && count > 0) {
            throw new BusinessException(409, "Process route is already referenced by production plans and cannot be deleted");
        }
        routeStepMapper.delete(Wrappers.<RouteStep>lambdaQuery().eq(RouteStep::getRouteId, routeId));
        processRouteMapper.deleteById(routeId);
    }

    public List<RouteStepVo> listRouteSteps(Long routeId) {
        requireRoute(routeId);
        List<RouteStep> routeSteps = routeStepMapper.selectList(Wrappers.<RouteStep>lambdaQuery()
                .eq(RouteStep::getRouteId, routeId)
                .orderByAsc(RouteStep::getSortOrder));
        if (routeSteps.isEmpty()) {
            return Collections.emptyList();
        }
        List<Long> stepIds = routeSteps.stream().map(RouteStep::getStepId).toList();
        Map<Long, ProcessStep> stepMap = processStepMapper.selectBatchIds(stepIds).stream()
                .collect(Collectors.toMap(ProcessStep::getStepId, Function.identity()));
        return routeSteps.stream().map(item -> {
            RouteStepVo vo = new RouteStepVo();
            vo.setRouteStepId(item.getRouteStepId());
            vo.setRouteId(item.getRouteId());
            vo.setStepId(item.getStepId());
            vo.setSortOrder(item.getSortOrder());
            vo.setIsMandatory(item.getIsMandatory());
            ProcessStep step = stepMap.get(item.getStepId());
            if (step != null) {
                vo.setStepCode(step.getStepCode());
                vo.setStepName(step.getStepName());
            }
            return vo;
        }).toList();
    }

    @Transactional
    public RouteStepVo createRouteStep(Long routeId, RouteStepUpsertRequest request) {
        requireRoute(routeId);
        ProcessStep step = requireStep(request.getStepId());
        RouteStep routeStep = new RouteStep();
        routeStep.setRouteId(routeId);
        routeStep.setStepId(request.getStepId());
        routeStep.setSortOrder(request.getSortOrder());
        routeStep.setIsMandatory(request.getIsMandatory());
        routeStepMapper.insert(routeStep);
        return toRouteStepVo(routeStep, step);
    }

    @Transactional
    public RouteStepVo updateRouteStep(Long routeId, Long routeStepId, RouteStepUpsertRequest request) {
        requireRoute(routeId);
        RouteStep routeStep = requireRouteStep(routeStepId);
        if (!routeId.equals(routeStep.getRouteId())) {
            throw new BusinessException(404, "Route step not found");
        }
        ProcessStep step = requireStep(request.getStepId());
        routeStep.setStepId(request.getStepId());
        routeStep.setSortOrder(request.getSortOrder());
        routeStep.setIsMandatory(request.getIsMandatory());
        routeStepMapper.updateById(routeStep);
        return toRouteStepVo(routeStep, step);
    }

    @Transactional
    public void deleteRouteStep(Long routeId, Long routeStepId) {
        requireRoute(routeId);
        RouteStep routeStep = requireRouteStep(routeStepId);
        if (!routeId.equals(routeStep.getRouteId())) {
            throw new BusinessException(404, "Route step not found");
        }
        routeStepMapper.deleteById(routeStepId);
    }

    public ProcessRoute requireRoute(Long routeId) {
        ProcessRoute route = processRouteMapper.selectById(routeId);
        if (route == null) {
            throw new BusinessException(404, "工艺路线不存在");
        }
        return route;
    }

    public ProcessStep requireStep(Long stepId) {
        ProcessStep step = processStepMapper.selectById(stepId);
        if (step == null) {
            throw new BusinessException(404, "Process step not found");
        }
        return step;
    }

    public RouteStep requireRouteStep(Long routeStepId) {
        RouteStep routeStep = routeStepMapper.selectById(routeStepId);
        if (routeStep == null) {
            throw new BusinessException(404, "Route step not found");
        }
        return routeStep;
    }

    public List<RouteStep> listRequiredRouteSteps(Long routeId) {
        List<RouteStep> routeSteps = routeStepMapper.selectList(Wrappers.<RouteStep>lambdaQuery()
                .eq(RouteStep::getRouteId, routeId)
                .orderByAsc(RouteStep::getSortOrder));
        if (routeSteps.isEmpty()) {
            throw new BusinessException(422, "工艺路线至少需要配置一个工序");
        }
        return routeSteps;
    }

    private RouteStepVo toRouteStepVo(RouteStep routeStep, ProcessStep step) {
        RouteStepVo vo = new RouteStepVo();
        vo.setRouteStepId(routeStep.getRouteStepId());
        vo.setRouteId(routeStep.getRouteId());
        vo.setStepId(routeStep.getStepId());
        vo.setSortOrder(routeStep.getSortOrder());
        vo.setIsMandatory(routeStep.getIsMandatory());
        vo.setStepCode(step.getStepCode());
        vo.setStepName(step.getStepName());
        return vo;
    }
}
