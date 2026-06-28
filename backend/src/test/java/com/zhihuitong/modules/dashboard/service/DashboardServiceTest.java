package com.zhihuitong.modules.dashboard.service;

import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.modules.batch.dto.BatchQuery;
import com.zhihuitong.modules.batch.service.BatchService;
import com.zhihuitong.modules.batch.vo.BatchListVo;
import com.zhihuitong.modules.dashboard.vo.DashboardOverviewVo;
import com.zhihuitong.modules.exception.dto.ExceptionRecordQuery;
import com.zhihuitong.modules.exception.entity.ExceptionRecord;
import com.zhihuitong.modules.exception.service.ExceptionRecordService;
import com.zhihuitong.modules.plan.dto.ProductionPlanQuery;
import com.zhihuitong.modules.plan.service.ProductionPlanService;
import com.zhihuitong.modules.plan.vo.ProductionPlanListVo;
import com.zhihuitong.modules.quality.dto.QcRecordQuery;
import com.zhihuitong.modules.quality.entity.QcRecord;
import com.zhihuitong.modules.quality.service.QcRecordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {
    @Mock private BatchService batchService;
    @Mock private QcRecordService qcRecordService;
    @Mock private ExceptionRecordService exceptionRecordService;
    @Mock private ProductionPlanService productionPlanService;

    private DashboardService dashboardService;

    @BeforeEach
    void setUp() {
        dashboardService = new DashboardService(
                batchService, qcRecordService, exceptionRecordService, productionPlanService);
    }

    @Test
    void overviewAggregatesScopedBusinessServiceResults() {
        BatchListVo pending = new BatchListVo();
        pending.setBatchId(1L);
        pending.setStatus("READY");
        BatchListVo completed = new BatchListVo();
        completed.setBatchId(2L);
        completed.setStatus("DONE");

        when(batchService.list(any(BatchQuery.class)))
                .thenReturn(table(List.of(pending, completed)))
                .thenReturn(table(List.of(pending)))
                .thenReturn(table(List.of(completed)));

        QcRecord qcRecord = new QcRecord();
        qcRecord.setInspectionId(3L);
        when(qcRecordService.list(any(QcRecordQuery.class))).thenReturn(table(List.of(qcRecord)));

        ExceptionRecord exceptionRecord = new ExceptionRecord();
        exceptionRecord.setExceptionId(4L);
        when(exceptionRecordService.list(any(ExceptionRecordQuery.class)))
                .thenReturn(table(List.of(exceptionRecord)));

        ProductionPlanListVo productionPlan = new ProductionPlanListVo();
        productionPlan.setPlanId(5L);
        when(productionPlanService.list(any(ProductionPlanQuery.class)))
                .thenReturn(table(List.of(productionPlan)));

        DashboardOverviewVo result = dashboardService.overview();

        assertThat(result.getPendingBatches()).extracting(BatchListVo::getBatchId).containsExactly(1L);
        assertThat(result.getTodayPendingCount()).isEqualTo(1);
        assertThat(result.getYesterdayPendingCount()).isZero();
        assertThat(result.getCurrentWeekQcRecords()).hasSize(1);
        assertThat(result.getPreviousWeekQcRecords()).hasSize(1);
        assertThat(result.getRecentQcRecords()).hasSize(1);
        assertThat(result.getExceptionRecords()).hasSize(1);
        assertThat(result.getProductionPlans()).hasSize(1);

        verify(batchService, times(3)).list(any(BatchQuery.class));
        verify(qcRecordService, times(3)).list(any(QcRecordQuery.class));
        verify(exceptionRecordService).list(any(ExceptionRecordQuery.class));
        verify(productionPlanService).list(any(ProductionPlanQuery.class));
    }

    private <T> TableDataInfo<T> table(List<T> rows) {
        return TableDataInfoBuilder.build(rows, rows.size());
    }
}