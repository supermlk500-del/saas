package com.zhihuitong.modules.process.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.enums.DeviceStatus;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.util.AuditRemarkUtils;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.process.dto.MachineQuery;
import com.zhihuitong.modules.process.dto.MachineStatusPatchRequest;
import com.zhihuitong.modules.process.dto.MachineUpsertRequest;
import com.zhihuitong.modules.process.entity.Machine;
import com.zhihuitong.modules.process.mapper.MachineMapper;
import com.zhihuitong.modules.process.vo.MachineVo;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class MachineService {

    private final MachineMapper machineMapper;

    public MachineService(MachineMapper machineMapper) {
        this.machineMapper = machineMapper;
    }

    public TableDataInfo<MachineVo> list(MachineQuery query) {
        Integer statusCode = StringUtils.hasText(query.getStatus()) ? DeviceStatus.toCode(query.getStatus()) : null;
        Page<Machine> page = machineMapper.selectPage(query.toPage(), Wrappers.<Machine>lambdaQuery()
                .like(StringUtils.hasText(query.getMachineCode()), Machine::getMachineCode, query.getMachineCode())
                .like(StringUtils.hasText(query.getMachineName()), Machine::getMachineName, query.getMachineName())
                .eq(StringUtils.hasText(query.getMachineType()), Machine::getMachineType, query.getMachineType())
                .eq(statusCode != null, Machine::getStatus, statusCode)
                .orderByDesc(Machine::getCreateTime));
        List<MachineVo> rows = page.getRecords().stream().map(this::toVo).toList();
        return TableDataInfoBuilder.build(rows, page.getTotal());
    }

    public MachineVo getDetail(Long machineId) {
        return toVo(requireMachine(machineId));
    }

    @Transactional
    public MachineVo create(MachineUpsertRequest request) {
        checkMachineCodeUnique(request.getMachineCode(), null);
        Machine machine = new Machine();
        copyRequest(request, machine);
        machine.setCreateTime(LocalDateTime.now());
        machineMapper.insert(machine);
        return toVo(machine);
    }

    @Transactional
    public MachineVo update(Long machineId, MachineUpsertRequest request) {
        Machine machine = requireMachine(machineId);
        checkMachineCodeUnique(request.getMachineCode(), machineId);
        copyRequest(request, machine);
        machineMapper.updateById(machine);
        return toVo(machine);
    }

    @Transactional
    public MachineVo patchStatus(Long machineId, MachineStatusPatchRequest request) {
        Machine machine = requireMachine(machineId);
        machine.setStatus(DeviceStatus.toCode(request.getStatus()));
        if (StringUtils.hasText(request.getReason())) {
            machine.setDescription(AuditRemarkUtils.append(machine.getDescription(), "STATUS_CHANGE", request.getReason()));
        }
        machineMapper.updateById(machine);
        return toVo(machine);
    }

    public Machine requireMachine(Long machineId) {
        Machine machine = machineMapper.selectById(machineId);
        if (machine == null) {
            throw new BusinessException(404, "Machine not found");
        }
        return machine;
    }

    private void copyRequest(MachineUpsertRequest request, Machine machine) {
        machine.setMachineCode(request.getMachineCode().trim());
        machine.setMachineName(request.getMachineName().trim());
        machine.setMachineType(request.getMachineType());
        machine.setDescription(request.getDescription());
        machine.setStatus(DeviceStatus.toCode(request.getStatus()));
    }

    private void checkMachineCodeUnique(String machineCode, Long excludeId) {
        Long count = machineMapper.selectCount(Wrappers.<Machine>lambdaQuery()
                .eq(Machine::getMachineCode, machineCode)
                .ne(excludeId != null, Machine::getMachineId, excludeId));
        if (count != null && count > 0) {
            throw new BusinessException(409, "Machine code already exists");
        }
    }

    private MachineVo toVo(Machine machine) {
        MachineVo vo = new MachineVo();
        vo.setMachineId(machine.getMachineId());
        vo.setMachineCode(machine.getMachineCode());
        vo.setMachineName(machine.getMachineName());
        vo.setMachineType(machine.getMachineType());
        vo.setDescription(machine.getDescription());
        vo.setStatus(DeviceStatus.fromCode(machine.getStatus()));
        vo.setCreateTime(machine.getCreateTime());
        return vo;
    }
}
