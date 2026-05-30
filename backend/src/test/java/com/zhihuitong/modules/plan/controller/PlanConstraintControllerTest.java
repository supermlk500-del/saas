package com.zhihuitong.modules.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.GlobalExceptionHandler;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.config.JacksonConfig;
import com.zhihuitong.modules.plan.service.PlanConstraintService;
import com.zhihuitong.modules.plan.vo.PlanConstraintVo;
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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlanConstraintControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlanConstraintService planConstraintService;

    @BeforeEach
    void setUp() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jackson2ObjectMapperBuilderCustomizer().customize(builder);
        ObjectMapper objectMapper = builder.build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PlanConstraintController(planConstraintService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listShouldSerializeConstraintRows() throws Exception {
        PlanConstraintVo constraint = new PlanConstraintVo();
        constraint.setKey("machine_capability");
        constraint.setName("机台能力匹配");
        constraint.setScope("候选机台");
        constraint.setConstraintType("硬约束");
        constraint.setHardConstraint(true);
        constraint.setPriority(10);
        constraint.setRule("工序必须存在启用的机台能力");
        constraint.setViolationResult("违反后工序进入未分配原因列表");
        constraint.setSuggestion("维护工序-机台能力参数");
        constraint.setStatus("启用");
        TableDataInfo<PlanConstraintVo> table = TableDataInfoBuilder.build(List.of(constraint), 1);

        when(planConstraintService.list(any())).thenReturn(table);

        mockMvc.perform(get("/api/plan-constraints")
                        .param("keyword", "machine"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].key").value("machine_capability"))
                .andExpect(jsonPath("$.rows[0].hardConstraint").value(true))
                .andExpect(jsonPath("$.total").value(1));
    }
}
