package com.zhihuitong.modules.plan.unassigned;

import com.zhihuitong.modules.plan.vo.PlanStepVo;
import com.zhihuitong.modules.plan.vo.PlanUnassignedReasonItemVo;
import com.zhihuitong.modules.plan.vo.PlanUnassignedReasonVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Service
public class PlanUnassignedReasonService {

    public PlanUnassignedReasonVo analyze(Long planId, List<PlanStepVo> steps) {
        List<PlanUnassignedReasonItemVo> items = new ArrayList<>();
        int unassignedCount = 0;
        int abnormalCount = 0;
        int missingTimeCount = 0;

        for (PlanStepVo step : steps) {
            Reason reason = resolveReason(step);
            if (reason == null) {
                continue;
            }
            if ("MACHINE_UNASSIGNED".equals(reason.code())) {
                unassignedCount++;
            } else if ("STEP_ABNORMAL".equals(reason.code())) {
                abnormalCount++;
            } else if ("TIME_WINDOW_MISSING".equals(reason.code())) {
                missingTimeCount++;
            }
            items.add(toItem(step, reason));
        }

        PlanUnassignedReasonVo result = new PlanUnassignedReasonVo();
        result.setPlanId(planId);
        result.setTotalIssueCount(items.size());
        result.setUnassignedCount(unassignedCount);
        result.setAbnormalCount(abnormalCount);
        result.setMissingTimeCount(missingTimeCount);
        result.setItems(items);
        return result;
    }

    private Reason resolveReason(PlanStepVo step) {
        if (step.getMachineId() == null) {
            return new Reason(
                    "MACHINE_UNASSIGNED",
                    StringUtils.hasText(step.getRemark()) ? step.getRemark() : "当前工序未分配机台",
                    "检查该工序是否存在启用的机台能力，或补充批次宽幅/重量匹配的设备能力"
            );
        }
        if ("ABNORMAL".equals(step.getStatus())) {
            return new Reason(
                    "STEP_ABNORMAL",
                    StringUtils.hasText(step.getRemark()) ? step.getRemark() : "当前工序状态异常",
                    "查看异常记录并确认是否需要重新分配机台或调整工序时间"
            );
        }
        if (step.getPlanStartTime() == null || step.getPlanEndTime() == null) {
            return new Reason(
                    "TIME_WINDOW_MISSING",
                    "当前工序缺少计划开始或结束时间",
                    "补充工序计划时间，或重新生成该生产计划的工序排程"
            );
        }
        return null;
    }

    private PlanUnassignedReasonItemVo toItem(PlanStepVo step, Reason reason) {
        PlanUnassignedReasonItemVo item = new PlanUnassignedReasonItemVo();
        item.setPlanStepId(step.getPlanStepId());
        item.setStepId(step.getStepId());
        item.setStepName(step.getStepName());
        item.setSequenceNo(step.getSequenceNo());
        item.setStatus(step.getStatus());
        item.setMachineId(step.getMachineId());
        item.setReasonCode(reason.code());
        item.setReasonDesc(reason.description());
        item.setSuggestion(reason.suggestion());
        item.setRemark(step.getRemark());
        return item;
    }

    private record Reason(String code, String description, String suggestion) {
    }
}
