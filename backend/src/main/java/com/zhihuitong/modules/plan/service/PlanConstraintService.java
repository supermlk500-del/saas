package com.zhihuitong.modules.plan.service;

import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.plan.dto.PlanConstraintQuery;
import com.zhihuitong.modules.plan.vo.PlanConstraintVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Service
public class PlanConstraintService {

    private static final String ENABLED = "启用";
    private static final String HARD = "硬约束";
    private static final String SOFT = "软约束";

    private static final List<PlanConstraintVo> CONSTRAINTS = List.of(
            constraint(
                    "machine_capability",
                    "机台能力匹配",
                    "候选机台",
                    HARD,
                    10,
                    "工序必须存在启用的机台能力，且批次宽幅/重量落在能力范围内",
                    "违反后工序进入未分配原因列表",
                    "维护工序-机台能力参数，补充宽幅、速度、最大批重等信息"
            ),
            constraint(
                    "machine_status",
                    "机台状态可用",
                    "候选机台",
                    HARD,
                    20,
                    "维修、停用等不可用机台不能被自动选中",
                    "违反后跳过该候选机台",
                    "确认设备状态，或将可用设备状态调整为空闲/运行"
            ),
            constraint(
                    "due_date",
                    "交期限制",
                    "计划结果",
                    HARD,
                    30,
                    "计划完工时间应尽量不晚于订单交期",
                    "违反后记录延期或进入未分配原因列表",
                    "调整计划开始时间、提升机台速度，或拆分为更细批次"
            ),
            constraint(
                    "manual_lock",
                    "人工锁定",
                    "任务前置",
                    HARD,
                    40,
                    "已锁定订单或工序不能被自动重排覆盖",
                    "违反后任务不参与自动重排",
                    "解除锁定后再执行重排，或手动指定机台和时间"
            ),
            constraint(
                    "continuous_limit",
                    "连续生产限制",
                    "任务前置",
                    HARD,
                    50,
                    "限制最小起排批量、连续同色/同工艺等生产边界",
                    "违反后任务进入未分配原因列表",
                    "调整批量、拆分任务，或放宽连续生产规则"
            ),
            constraint(
                    "changeover",
                    "换缸/换型代价",
                    "候选机台",
                    SOFT,
                    60,
                    "换缸、换型、清洗会增加额外准备工时",
                    "违反后记录软约束警告并参与机台评分",
                    "优先选择同色同工艺连续生产的机台，减少切换时间"
            ),
            constraint(
                    "dye_color_changeover",
                    "染色换色规则",
                    "候选机台",
                    SOFT,
                    70,
                    "深浅色切换、洗缸要求和颜色专用缸影响机台选择",
                    "违反后记录软约束警告或提高换色代价",
                    "维护颜色分组和专用缸规则，优先安排相近颜色连续生产"
            )
    );

    public TableDataInfo<PlanConstraintVo> list(PlanConstraintQuery query) {
        List<PlanConstraintVo> rows = CONSTRAINTS.stream()
                .filter(item -> matchesKeyword(item, query.getKeyword()))
                .filter(item -> matches(item.getScope(), query.getScope()))
                .filter(item -> matches(item.getConstraintType(), query.getConstraintType()))
                .filter(item -> matches(item.getStatus(), query.getStatus()))
                .toList();
        return TableDataInfoBuilder.build(rows, rows.size());
    }

    private boolean matchesKeyword(PlanConstraintVo item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String value = keyword.trim().toLowerCase(Locale.ROOT);
        return item.getKey().toLowerCase(Locale.ROOT).contains(value)
                || item.getName().toLowerCase(Locale.ROOT).contains(value)
                || item.getRule().toLowerCase(Locale.ROOT).contains(value)
                || item.getSuggestion().toLowerCase(Locale.ROOT).contains(value);
    }

    private boolean matches(String value, String expected) {
        return !StringUtils.hasText(expected) || value.equals(expected);
    }

    private static PlanConstraintVo constraint(String key,
                                               String name,
                                               String scope,
                                               String constraintType,
                                               int priority,
                                               String rule,
                                               String violationResult,
                                               String suggestion) {
        PlanConstraintVo item = new PlanConstraintVo();
        item.setKey(key);
        item.setName(name);
        item.setScope(scope);
        item.setConstraintType(constraintType);
        item.setHardConstraint(HARD.equals(constraintType));
        item.setPriority(priority);
        item.setRule(rule);
        item.setViolationResult(violationResult);
        item.setSuggestion(suggestion);
        item.setStatus(ENABLED);
        return item;
    }
}
