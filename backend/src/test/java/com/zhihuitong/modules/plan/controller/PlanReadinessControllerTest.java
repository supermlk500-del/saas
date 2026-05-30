package com.zhihuitong.modules.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuitong.common.exception.GlobalExceptionHandler;
import com.zhihuitong.config.JacksonConfig;
import com.zhihuitong.modules.plan.service.PlanReadinessService;
import com.zhihuitong.modules.plan.vo.PlanReadinessIssueVo;
import com.zhihuitong.modules.plan.vo.PlanReadinessVo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlanReadinessControllerTest {

    private static final long LONG_PLAN_ID = 2058721876820766721L;

    private MockMvc mockMvc;

    @Mock
    private PlanReadinessService planReadinessService;

    @BeforeEach
    void setUp() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jackson2ObjectMapperBuilderCustomizer().customize(builder);
        ObjectMapper objectMapper = builder.build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PlanReadinessController(planReadinessService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void checkShouldSerializeReadinessResult() throws Exception {
        PlanReadinessIssueVo issue = new PlanReadinessIssueVo();
        issue.setSeverity("BLOCKER");
        issue.setCode("MACHINE_UNASSIGNED");
        issue.setPlanStepId(6001L);
        issue.setMessage("工序未分配机台");
        issue.setSuggestion("先为该工序分配支持当前批次的可用机台");

        PlanReadinessVo result = new PlanReadinessVo();
        result.setPlanId(LONG_PLAN_ID);
        result.setReady(false);
        result.setTotalStepCount(1);
        result.setBlockerCount(1);
        result.setWarningCount(0);
        result.setIssues(List.of(issue));

        when(planReadinessService.check(LONG_PLAN_ID)).thenReturn(result);

        mockMvc.perform(get("/api/plan-readiness/{planId}", String.valueOf(LONG_PLAN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planId").value(String.valueOf(LONG_PLAN_ID)))
                .andExpect(jsonPath("$.data.ready").value(false))
                .andExpect(jsonPath("$.data.issues[0].code").value("MACHINE_UNASSIGNED"));
    }
}
