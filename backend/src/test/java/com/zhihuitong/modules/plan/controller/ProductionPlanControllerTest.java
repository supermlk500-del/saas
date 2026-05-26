package com.zhihuitong.modules.plan.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.common.exception.BusinessException;
import com.zhihuitong.common.exception.GlobalExceptionHandler;
import com.zhihuitong.common.util.TableDataInfoBuilder;
import com.zhihuitong.config.JacksonConfig;
import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.entity.OrderItem;
import com.zhihuitong.modules.plan.service.ProductionPlanService;
import com.zhihuitong.modules.plan.vo.PlanStepVo;
import com.zhihuitong.modules.plan.vo.ProductionPlanDetailVo;
import com.zhihuitong.modules.plan.vo.ProductionPlanListVo;
import com.zhihuitong.modules.process.entity.ProcessRoute;
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
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ProductionPlanControllerTest {

    private static final long LONG_PLAN_ID = 2058721876820766721L;

    private MockMvc mockMvc;

    @Mock
    private ProductionPlanService productionPlanService;

    @BeforeEach
    void setUp() {
        Jackson2ObjectMapperBuilder builder = new Jackson2ObjectMapperBuilder();
        new JacksonConfig().jackson2ObjectMapperBuilderCustomizer().customize(builder);
        ObjectMapper objectMapper = builder.build();
        mockMvc = MockMvcBuilders
                .standaloneSetup(new ProductionPlanController(productionPlanService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(objectMapper))
                .build();
    }

    @Test
    void listShouldSerializeLongPlanIdAsString() throws Exception {
        ProductionPlanListVo row = new ProductionPlanListVo();
        row.setPlanId(LONG_PLAN_ID);
        row.setOrderId(2001L);
        row.setOrderNo("ORD-001");
        row.setCustomerName("customer-a");
        row.setOrderItemId(3001L);
        row.setProductCode("P-001");
        row.setProductName("product-a");
        row.setSpecification("100D");
        row.setColor("black");
        row.setBatchId(4001L);
        row.setBatchNo("B-001");
        row.setRouteId(5001L);
        row.setRouteName("route-a");
        TableDataInfo<ProductionPlanListVo> table = TableDataInfoBuilder.build(List.of(row), 1L);

        when(productionPlanService.list(any())).thenReturn(table);

        mockMvc.perform(get("/api/production-plans")
                        .param("pageNum", "1")
                        .param("pageSize", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.rows[0].planId").value(String.valueOf(LONG_PLAN_ID)))
                .andExpect(jsonPath("$.rows[0].orderItemId").value("3001"))
                .andExpect(jsonPath("$.rows[0].batchId").value("4001"));
    }

    @Test
    void detailShouldFindPlanByLongStringIdAndSerializeAsString() throws Exception {
        ProductionPlanDetailVo detail = new ProductionPlanDetailVo();
        detail.setPlanId(LONG_PLAN_ID);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderId(2001L);
        orderInfo.setOrderNo("ORD-001");
        orderInfo.setCustomerName("customer-a");
        detail.setOrderInfo(orderInfo);

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderItemId(3001L);
        orderItem.setProductCode("P-001");
        orderItem.setProductName("product-a");
        detail.setOrderItemInfo(orderItem);

        BatchInfo batchInfo = new BatchInfo();
        batchInfo.setBatchId(4001L);
        batchInfo.setBatchNo("B-001");
        detail.setBatchInfo(batchInfo);

        ProcessRoute route = new ProcessRoute();
        route.setRouteId(5001L);
        route.setRouteName("route-a");
        detail.setRouteInfo(route);

        PlanStepVo planStep = new PlanStepVo();
        planStep.setPlanStepId(6001L);
        planStep.setPlanId(LONG_PLAN_ID);
        planStep.setStepId(7001L);
        planStep.setStatus("PENDING");
        detail.setPlanSteps(List.of(planStep));

        when(productionPlanService.getDetail(LONG_PLAN_ID)).thenReturn(detail);

        mockMvc.perform(get("/api/production-plans/{planId}", String.valueOf(LONG_PLAN_ID)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.planId").value(String.valueOf(LONG_PLAN_ID)))
                .andExpect(jsonPath("$.data.orderInfo.orderId").value("2001"))
                .andExpect(jsonPath("$.data.planSteps[0].planId").value(String.valueOf(LONG_PLAN_ID)));

        verify(productionPlanService).getDetail(LONG_PLAN_ID);
    }

    @Test
    void detailShouldReturnReal404WithChineseMessage() throws Exception {
        String message = "\u751f\u4ea7\u8ba1\u5212\u4e0d\u5b58\u5728";
        when(productionPlanService.getDetail(LONG_PLAN_ID))
                .thenThrow(new BusinessException(404, message));

        mockMvc.perform(get("/api/production-plans/{planId}", String.valueOf(LONG_PLAN_ID)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.msg").value(message));
    }
}
