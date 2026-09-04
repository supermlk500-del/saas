package com.zhihuitong.modules.batch.service;

import com.zhihuitong.modules.batch.dto.BatchUpsertRequest;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.mapper.BatchInfoMapper;
import com.zhihuitong.modules.exception.mapper.ExceptionRecordMapper;
import com.zhihuitong.modules.plan.mapper.PlanStepMapper;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import com.zhihuitong.modules.quality.mapper.QcRecordMapper;
import com.zhihuitong.security.service.DataScopeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchServiceTest {

    @Mock
    private BatchInfoMapper batchInfoMapper;

    @Mock
    private ProductionPlanMapper productionPlanMapper;

    @Mock
    private PlanStepMapper planStepMapper;

    @Mock
    private QcRecordMapper qcRecordMapper;

    @Mock
    private ExceptionRecordMapper exceptionRecordMapper;

    @Mock
    private BatchResourcePoolService batchResourcePoolService;

    @Mock
    private DataScopeService dataScopeService;

    private BatchService service;

    @BeforeEach
    void setUp() {
        service = new BatchService(
                batchInfoMapper,
                productionPlanMapper,
                planStepMapper,
                qcRecordMapper,
                exceptionRecordMapper,
                batchResourcePoolService,
                dataScopeService
        );
    }

    @Test
    void create_setsAuditFieldsFromCurrentUserContext() {
        BatchUpsertRequest request = new BatchUpsertRequest();
        request.setBatchNo("B20260830-001");
        request.setSupplier("江苏华源纺织");
        request.setInDate(LocalDateTime.of(2026, 8, 30, 8, 30));
        request.setWeight(new BigDecimal("2200"));
        request.setWidth(new BigDecimal("180"));
        request.setComposition("CVC 60/40");
        request.setNote("演示批次");

        when(batchInfoMapper.selectCount(any())).thenReturn(0L);
        when(dataScopeService.currentDeptId()).thenReturn(20L);
        when(dataScopeService.currentUserId()).thenReturn(30L);
        doAnswer(invocation -> {
            BatchInfo entity = invocation.getArgument(0);
            entity.setBatchId(100L);
            return 1;
        }).when(batchInfoMapper).insert(any(BatchInfo.class));

        service.create(request);

        ArgumentCaptor<BatchInfo> captor = ArgumentCaptor.forClass(BatchInfo.class);
        verify(batchInfoMapper).insert(captor.capture());
        BatchInfo inserted = captor.getValue();
        assertThat(inserted.getDeptId()).isEqualTo(20L);
        assertThat(inserted.getCreatedBy()).isEqualTo(30L);
        assertThat(inserted.getBatchNo()).isEqualTo("B20260830-001");
    }
}
