package com.zhihuitong.modules.plan.service;

import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.plan.dto.PlanStrategyQuery;
import com.zhihuitong.modules.plan.vo.PlanStrategyVo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanStrategyServiceTest {

    private final PlanStrategyService service = new PlanStrategyService();

    @Test
    void listShouldReturnEnabledSchedulingStrategies() {
        TableDataInfo<PlanStrategyVo> result = service.list(new PlanStrategyQuery());

        assertThat(result.getRows())
                .extracting("key")
                .contains("greedy_eft", "edd", "critical_ratio", "changeover_first", "composite");
        assertThat(result.getTotal()).isEqualTo(result.getRows().size());
    }

    @Test
    void listShouldFilterByKeyword() {
        PlanStrategyQuery query = new PlanStrategyQuery();
        query.setKeyword("交期");

        TableDataInfo<PlanStrategyVo> result = service.list(query);

        assertThat(result.getRows()).extracting("key").containsExactly("edd", "composite");
    }
}
