package com.zhihuitong.modules.plan.controller;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

import static org.assertj.core.api.Assertions.assertThat;

class PlanStepPermissionTest {
    @Test
    void readEndpointsAllowPlanningOrRealtimeQualityPermission() throws Exception {
        String expected = "@auth.hasAnyPermission('plan:production:list', 'quality:realtime:view')";

        PreAuthorize listPermission = PlanStepController.class
                .getMethod("list", com.zhihuitong.modules.plan.dto.PlanStepQuery.class)
                .getAnnotation(PreAuthorize.class);
        PreAuthorize detailPermission = PlanStepController.class
                .getMethod("detail", Long.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(listPermission.value()).isEqualTo(expected);
        assertThat(detailPermission.value()).isEqualTo(expected);
    }

    @Test
    void writeEndpointsStillRequirePlanningEditPermission() throws Exception {
        PreAuthorize updatePermission = PlanStepController.class
                .getMethod("update", Long.class, com.zhihuitong.modules.plan.dto.PlanStepUpdateRequest.class)
                .getAnnotation(PreAuthorize.class);
        PreAuthorize machinePermission = PlanStepController.class
                .getMethod("patchMachine", Long.class, com.zhihuitong.modules.plan.dto.PlanStepMachinePatchRequest.class)
                .getAnnotation(PreAuthorize.class);
        PreAuthorize statusPermission = PlanStepController.class
                .getMethod("patchStatus", Long.class, com.zhihuitong.modules.plan.dto.StatusPatchRequest.class)
                .getAnnotation(PreAuthorize.class);

        assertThat(updatePermission.value()).isEqualTo("@auth.hasPermission('plan:production:edit')");
        assertThat(machinePermission.value()).isEqualTo("@auth.hasPermission('plan:production:edit')");
        assertThat(statusPermission.value()).isEqualTo("@auth.hasPermission('plan:production:edit')");
    }
}