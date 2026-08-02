package com.zhihuitong.modules.quality.service;

import com.zhihuitong.modules.exception.mapper.ExceptionRecordMapper;
import com.zhihuitong.modules.plan.service.PlanStepService;
import com.zhihuitong.modules.quality.dto.QcRecordUpsertRequest;
import com.zhihuitong.modules.quality.entity.QcRecord;
import com.zhihuitong.modules.quality.mapper.QcRecordMapper;
import com.zhihuitong.security.service.DataScopeService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class QcRecordServiceTest {

    @Test
    void createFillsRequiredDataScopeFields() {
        QcRecordMapper recordMapper = mock(QcRecordMapper.class);
        QcItemService itemService = mock(QcItemService.class);
        PlanStepService planStepService = mock(PlanStepService.class);
        QcCameraService cameraService = mock(QcCameraService.class);
        ExceptionRecordMapper exceptionMapper = mock(ExceptionRecordMapper.class);
        DataScopeService dataScopeService = mock(DataScopeService.class);
        when(dataScopeService.currentDeptId()).thenReturn(103L);
        when(dataScopeService.currentUserId()).thenReturn(1L);

        QcRecordService service = new QcRecordService(
                recordMapper,
                itemService,
                planStepService,
                cameraService,
                exceptionMapper,
                dataScopeService
        );
        QcRecordUpsertRequest request = new QcRecordUpsertRequest();
        request.setPlanStepId(10L);
        request.setQcItemId(20L);
        request.setInspectTime(LocalDateTime.of(2026, 7, 17, 15, 0));
        request.setInspectType("offline");
        request.setResultJudge("PASS");

        service.create(request);

        ArgumentCaptor<QcRecord> captor = ArgumentCaptor.forClass(QcRecord.class);
        verify(recordMapper).insert(captor.capture());
        assertThat(captor.getValue().getDeptId()).isEqualTo(103L);
        assertThat(captor.getValue().getCreatedBy()).isEqualTo(1L);
    }
}
