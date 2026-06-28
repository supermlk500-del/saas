package com.zhihuitong.modules.dashboard.service;

import com.zhihuitong.common.domain.TableDataInfo;
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
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Set;

@Service
public class DashboardService {
    private static final long DASHBOARD_PAGE_SIZE = 200;
    private static final Set<String> PENDING_BATCH_STATUSES = Set.of("NEW", "READY");

    private final BatchService batchService;
    private final QcRecordService qcRecordService;
    private final ExceptionRecordService exceptionRecordService;
    private final ProductionPlanService productionPlanService;

    public DashboardService(BatchService batchService, QcRecordService qcRecordService,
                            ExceptionRecordService exceptionRecordService,
                            ProductionPlanService productionPlanService) {
        this.batchService = batchService;
        this.qcRecordService = qcRecordService;
        this.exceptionRecordService = exceptionRecordService;
        this.productionPlanService = productionPlanService;
    }

    public DashboardOverviewVo overview() {
        LocalDate today = LocalDate.now();
        LocalDate currentWeekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate currentWeekEnd = currentWeekStart.plusDays(6);
        LocalDate previousWeekStart = currentWeekStart.minusWeeks(1);
        LocalDate previousWeekEnd = currentWeekStart.minusDays(1);

        List<BatchListVo> allBatches = batchService.list(batchQuery(null, null)).getRows();
        List<BatchListVo> pendingBatches = allBatches.stream()
                .filter(item -> PENDING_BATCH_STATUSES.contains(item.getStatus()))
                .toList();

        DashboardOverviewVo result = new DashboardOverviewVo();
        result.setPendingBatches(pendingBatches);
        result.setTodayPendingCount(countPending(batchService.list(batchQuery(startOf(today), endOf(today)))));
        LocalDate yesterday = today.minusDays(1);
        result.setYesterdayPendingCount(countPending(batchService.list(batchQuery(startOf(yesterday), endOf(yesterday)))));
        result.setCurrentWeekQcRecords(qcRecordService.list(qcQuery(startOf(currentWeekStart), endOf(currentWeekEnd))).getRows());
        result.setPreviousWeekQcRecords(qcRecordService.list(qcQuery(startOf(previousWeekStart), endOf(previousWeekEnd))).getRows());
        result.setRecentQcRecords(qcRecordService.list(qcQuery(null, null, 20)).getRows());
        result.setExceptionRecords(exceptionRecordService.list(exceptionQuery()).getRows());
        result.setProductionPlans(productionPlanService.list(planQuery()).getRows());
        return result;
    }

    private long countPending(TableDataInfo<BatchListVo> data) {
        return data.getRows().stream()
                .filter(item -> PENDING_BATCH_STATUSES.contains(item.getStatus()))
                .count();
    }

    private BatchQuery batchQuery(LocalDateTime from, LocalDateTime to) {
        BatchQuery query = new BatchQuery();
        query.setPageSize(DASHBOARD_PAGE_SIZE);
        query.setDateFrom(from);
        query.setDateTo(to);
        return query;
    }

    private QcRecordQuery qcQuery(LocalDateTime from, LocalDateTime to) {
        return qcQuery(from, to, DASHBOARD_PAGE_SIZE);
    }

    private QcRecordQuery qcQuery(LocalDateTime from, LocalDateTime to, long pageSize) {
        QcRecordQuery query = new QcRecordQuery();
        query.setPageSize(pageSize);
        query.setInspectTimeFrom(from);
        query.setInspectTimeTo(to);
        return query;
    }

    private ExceptionRecordQuery exceptionQuery() {
        ExceptionRecordQuery query = new ExceptionRecordQuery();
        query.setPageSize(DASHBOARD_PAGE_SIZE);
        return query;
    }

    private ProductionPlanQuery planQuery() {
        ProductionPlanQuery query = new ProductionPlanQuery();
        query.setPageSize(DASHBOARD_PAGE_SIZE);
        return query;
    }

    private LocalDateTime startOf(LocalDate date) {
        return date.atStartOfDay();
    }

    private LocalDateTime endOf(LocalDate date) {
        return date.atTime(LocalTime.MAX);
    }
}
