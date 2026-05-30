package com.zhihuitong.modules.plan.service;

import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.plan.dto.PlanConstraintQuery;
import com.zhihuitong.modules.plan.vo.PlanConstraintVo;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class PlanConstraintServiceTest {

    private final PlanConstraintService service = new PlanConstraintService();

    @Test
    void listShouldReturnHardAndSoftSchedulingConstraints() {
        TableDataInfo<PlanConstraintVo> result = service.list(new PlanConstraintQuery());

        assertThat(result.getRows())
                .extracting("key")
                .contains("machine_capability", "due_date", "manual_lock", "changeover", "dye_color_changeover");
        assertThat(result.getRows())
                .extracting("constraintType")
                .contains("硬约束", "软约束");
    }

    @Test
    void listShouldFilterByConstraintType() {
        PlanConstraintQuery query = new PlanConstraintQuery();
        query.setConstraintType("软约束");

        TableDataInfo<PlanConstraintVo> result = service.list(query);

        assertThat(result.getRows()).extracting("key").containsExactly("changeover", "dye_color_changeover");
        assertThat(result.getRows()).allMatch(item -> Boolean.FALSE.equals(item.getHardConstraint()));
    }

    @Test
    void listShouldFilterByKeyword() {
        PlanConstraintQuery query = new PlanConstraintQuery();
        query.setKeyword("锁定");

        TableDataInfo<PlanConstraintVo> result = service.list(query);

        assertThat(result.getRows()).extracting("key").containsExactly("manual_lock");
    }
}
