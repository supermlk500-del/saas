package com.zhihuitong.modules.dashboard.controller;

import com.zhihuitong.modules.dashboard.service.DashboardService;
import com.zhihuitong.modules.dashboard.vo.DashboardOverviewVo;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DashboardControllerTest {
    @Test
    void overviewUsesDashboardViewPermission() throws Exception {
        PreAuthorize annotation = DashboardController.class
                .getMethod("overview")
                .getAnnotation(PreAuthorize.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.value()).isEqualTo("@auth.hasPermission('dashboard:view')");
    }

    @Test
    void overviewReturnsDashboardServiceData() {
        DashboardService service = mock(DashboardService.class);
        DashboardOverviewVo overview = new DashboardOverviewVo();
        when(service.overview()).thenReturn(overview);

        new DashboardController(service).overview();

        verify(service).overview();
    }
}