package com.zhihuitong.modules.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.GlobalExceptionHandler;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.config.JacksonConfig;
import com.zhihuitong.modules.plan.service.PlanStrategyService;
import com.zhihuitong.modules.plan.vo.PlanStrategyVo;
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
class PlanStrategyControllerTest {

    private MockMvc mockMvc;

    @Mock
    private PlanStrategyService planStrategyService;

    @BeforeEach
    void setUp() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jackson2ObjectMapperBuilderCustomizer().customize(builder);
        ObjectMapper objectMapper = builder.build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PlanStrategyController(planStrategyService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listShouldSerializeStrategyRows() throws Exception {
        PlanStrategyVo strategy = new PlanStrategyVo();
        strategy.setKey("greedy_eft");
        strategy.setName("最早完工贪心");
        strategy.setRule("按预计最早完工时间选择候选机台");
        strategy.setPriority(10);
        strategy.setStatus("启用");
        TableDataInfo<PlanStrategyVo> table = TableDataInfoBuilder.build(List.of(strategy), 1);

        when(planStrategyService.list(any())).thenReturn(table);

        mockMvc.perform(get("/api/plan-strategies")
                        .param("keyword", "greedy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].key").value("greedy_eft"))
                .andExpect(jsonPath("$.rows[0].name").value("最早完工贪心"))
                .andExpect(jsonPath("$.total").value(1));
    }
}
