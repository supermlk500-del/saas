package com.zhihuitong.modules.dashboard.vo;

import com.zhihuitong.modules.batch.vo.BatchListVo;
import com.zhihuitong.modules.exception.entity.ExceptionRecord;
import com.zhihuitong.modules.plan.vo.ProductionPlanListVo;
import com.zhihuitong.modules.quality.entity.QcRecord;
import lombok.Data;

import java.util.List;

@Data
public class DashboardOverviewVo {
    private List<BatchListVo> pendingBatches;
    private long todayPendingCount;
    private long yesterdayPendingCount;
    private List<QcRecord> currentWeekQcRecords;
    private List<QcRecord> previousWeekQcRecords;
    private List<QcRecord> recentQcRecords;
    private List<ExceptionRecord> exceptionRecords;
    private List<ProductionPlanListVo> productionPlans;
}
