package com.zhihuitong.modules.plan.service;

import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.plan.dto.PlanStrategyQuery;
import com.zhihuitong.modules.plan.vo.PlanStrategyVo;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class PlanStrategyService {

    private static final String ENABLED = "启用";

    private static final List<PlanStrategyVo> STRATEGIES = List.of(
            strategy("greedy_eft", "最早完工贪心", "按预计最早完工时间选择候选机台", 10, ENABLED),
            strategy("priority_first", "优先级排序", "订单优先级高的任务先进入排产队列", 20, ENABLED),
            strategy("edd", "交期优先排序", "按订单交期从早到晚安排任务顺序", 30, ENABLED),
            strategy("critical_ratio", "关键比率排序", "按剩余时间与剩余工时比值识别更紧急任务", 40, ENABLED),
            strategy("changeover_first", "减少换缸评分", "优先选择换色/换缸代价更低的机台", 50, ENABLED),
            strategy("composite", "综合评分", "综合完工时间、换缸代价、交期紧急度和客户优先级评分", 60, ENABLED)
    );

    public TableDataInfo<PlanStrategyVo> list(PlanStrategyQuery query) {
        List<PlanStrategyVo> rows = STRATEGIES.stream()
                .filter(item -> matchesKeyword(item, query.getKeyword()))
                .filter(item -> !StringUtils.hasText(query.getStatus()) || item.getStatus().equals(query.getStatus()))
                .toList();
        return TableDataInfoBuilder.build(rows, rows.size());
    }

    private boolean matchesKeyword(PlanStrategyVo item, String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return true;
        }
        String value = keyword.trim().toLowerCase();
        return item.getKey().toLowerCase().contains(value)
                || item.getName().toLowerCase().contains(value)
                || item.getRule().toLowerCase().contains(value);
    }

    private static PlanStrategyVo strategy(String key, String name, String rule, int priority, String status) {
        PlanStrategyVo item = new PlanStrategyVo();
        item.setKey(key);
        item.setName(name);
        item.setRule(rule);
        item.setPriority(priority);
        item.setStatus(status);
        return item;
    }
}
