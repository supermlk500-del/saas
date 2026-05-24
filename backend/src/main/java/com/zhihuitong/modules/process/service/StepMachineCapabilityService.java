package com.zhihuitong.modules.process.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.process.dto.CapabilityQuery;
import com.zhihuitong.modules.process.dto.CapabilityUpsertRequest;
import com.zhihuitong.modules.process.entity.StepMachineCapability;
import com.zhihuitong.modules.process.mapper.StepMachineCapabilityMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
public class StepMachineCapabilityService {

    private final StepMachineCapabilityMapper capabilityMapper;
    private final ProcessStepService processStepService;
    private final MachineService machineService;

    public StepMachineCapabilityService(StepMachineCapabilityMapper capabilityMapper,
                                        ProcessStepService processStepService,
                                        MachineService machineService) {
        this.capabilityMapper = capabilityMapper;
        this.processStepService = processStepService;
        this.machineService = machineService;
    }

    public TableDataInfo<StepMachineCapability> list(CapabilityQuery query) {
        Page<StepMachineCapability> page = capabilityMapper.selectPage(query.toPage(), Wrappers.<StepMachineCapability>lambdaQuery()
                .eq(query.getStepId() != null, StepMachineCapability::getStepId, query.getStepId())
                .eq(query.getMachineId() != null, StepMachineCapability::getMachineId, query.getMachineId())
                .eq(query.getIsActive() != null, StepMachineCapability::getIsActive, query.getIsActive())
                .orderByDesc(StepMachineCapability::getCapId));
        return TableDataInfoBuilder.build(page);
    }

    @Transactional
    public StepMachineCapability create(CapabilityUpsertRequest request) {
        validateRequest(request);
        StepMachineCapability capability = new StepMachineCapability();
        copyRequest(request, capability);
        capabilityMapper.insert(capability);
        return capability;
    }

    @Transactional
    public StepMachineCapability update(Long capId, CapabilityUpsertRequest request) {
        validateRequest(request);
        StepMachineCapability capability = requireCapability(capId);
        copyRequest(request, capability);
        capabilityMapper.updateById(capability);
        return capability;
    }

    @Transactional
    public void delete(Long capId) {
        requireCapability(capId);
        capabilityMapper.deleteById(capId);
    }

    public StepMachineCapability requireCapability(Long capId) {
        StepMachineCapability capability = capabilityMapper.selectById(capId);
        if (capability == null) {
            throw new BusinessException(404, "Step machine capability not found");
        }
        return capability;
    }

    public List<StepMachineCapability> findActiveCapabilities(Long stepId) {
        return capabilityMapper.selectList(Wrappers.<StepMachineCapability>lambdaQuery()
                .eq(StepMachineCapability::getStepId, stepId)
                .eq(StepMachineCapability::getIsActive, 1));
    }

    public boolean supportsBatch(Long stepId, Long machineId, BigDecimal width, BigDecimal batchWeight) {
        List<StepMachineCapability> capabilities = capabilityMapper.selectList(Wrappers.<StepMachineCapability>lambdaQuery()
                .eq(StepMachineCapability::getStepId, stepId)
                .eq(StepMachineCapability::getMachineId, machineId)
                .eq(StepMachineCapability::getIsActive, 1));
        return capabilities.stream().anyMatch(item -> matches(item, width, batchWeight));
    }

    public boolean supportsCapability(StepMachineCapability capability, BigDecimal width, BigDecimal batchWeight) {
        return capability != null
                && Integer.valueOf(1).equals(capability.getIsActive())
                && matches(capability, width, batchWeight);
    }

    private void validateRequest(CapabilityUpsertRequest request) {
        processStepService.requireStep(request.getStepId());
        machineService.requireMachine(request.getMachineId());
        if (request.getMinWidth() != null && request.getMaxWidth() != null && request.getMinWidth().compareTo(request.getMaxWidth()) > 0) {
            throw new BusinessException(422, "minWidth must be less than or equal to maxWidth");
        }
    }

    private void copyRequest(CapabilityUpsertRequest request, StepMachineCapability capability) {
        capability.setStepId(request.getStepId());
        capability.setMachineId(request.getMachineId());
        capability.setMinWidth(request.getMinWidth());
        capability.setMaxWidth(request.getMaxWidth());
        capability.setMaxSpeed(request.getMaxSpeed());
        capability.setMaxBatchWeight(request.getMaxBatchWeight());
        capability.setIsActive(request.getIsActive());
    }

    private boolean matches(StepMachineCapability capability, BigDecimal width, BigDecimal batchWeight) {
        boolean widthMatched = width == null
                || ((capability.getMinWidth() == null || capability.getMinWidth().compareTo(width) <= 0)
                && (capability.getMaxWidth() == null || capability.getMaxWidth().compareTo(width) >= 0));
        boolean weightMatched = batchWeight == null
                || capability.getMaxBatchWeight() == null
                || capability.getMaxBatchWeight().compareTo(batchWeight) >= 0;
        return widthMatched && weightMatched;
    }
}
