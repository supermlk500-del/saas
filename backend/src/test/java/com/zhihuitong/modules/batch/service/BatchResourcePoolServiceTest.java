package com.zhihuitong.modules.batch.service;

import com.zhihuitong.modules.batch.entity.BatchInfo;
import com.zhihuitong.modules.batch.enums.BatchResourceStatusEnum;
import com.zhihuitong.modules.batch.mapper.BatchInfoMapper;
import com.zhihuitong.modules.batch.vo.BatchResourcePoolVo;
import com.zhihuitong.modules.order.entity.OrderBatchLink;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.entity.OrderItem;
import com.zhihuitong.modules.order.mapper.OrderBatchLinkMapper;
import com.zhihuitong.modules.order.mapper.OrderInfoMapper;
import com.zhihuitong.modules.order.mapper.OrderItemMapper;
import com.zhihuitong.modules.plan.mapper.ProductionPlanMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BatchResourcePoolServiceTest {

    @Mock
    private BatchInfoMapper batchInfoMapper;
    @Mock
    private OrderBatchLinkMapper orderBatchLinkMapper;
    @Mock
    private OrderInfoMapper orderInfoMapper;
    @Mock
    private OrderItemMapper orderItemMapper;
    @Mock
    private ProductionPlanMapper productionPlanMapper;

    private BatchResourcePoolService batchResourcePoolService;

    @BeforeEach
    void setUp() {
        batchResourcePoolService = new BatchResourcePoolService(
                batchInfoMapper,
                orderBatchLinkMapper,
                orderInfoMapper,
                orderItemMapper,
                productionPlanMapper
        );
    }

    @Test
    void getResourceShouldComputeQuantityFallbackWhenBatchHasNoWeight() {
        BatchInfo batch = new BatchInfo();
        batch.setBatchId(101L);
        batch.setBatchNo("B-101");

        OrderBatchLink link = new OrderBatchLink();
        link.setId(201L);
        link.setOrderId(301L);
        link.setOrderItemId(401L);
        link.setBatchId(101L);
        link.setAllocatedQuantity(new BigDecimal("60"));

        OrderInfo order = new OrderInfo();
        order.setOrderId(301L);
        order.setOrderNo("ORD-301");
        order.setStatus("READY");

        OrderItem item = new OrderItem();
        item.setOrderItemId(401L);
        item.setOrderId(301L);
        item.setProductCode("P-401");
        item.setProductName("fabric-a");
        item.setQuantity(new BigDecimal("100"));

        when(batchInfoMapper.selectById(101L)).thenReturn(batch);
        when(orderBatchLinkMapper.selectList(any())).thenReturn(List.of(link));
        when(productionPlanMapper.selectList(any())).thenReturn(List.of());
        when(orderInfoMapper.selectList(any())).thenReturn(List.of(order));
        when(orderItemMapper.selectList(any())).thenReturn(List.of(item));

        BatchResourcePoolVo resource = batchResourcePoolService.getResource(101L);

        assertThat(resource.getAllocatedQuantity()).isEqualByComparingTo("60");
        assertThat(resource.getRemainingQuantity()).isEqualByComparingTo("40");
        assertThat(resource.getResourceStatus()).isEqualTo(BatchResourceStatusEnum.PARTIALLY_ALLOCATED.getCode());
        assertThat(resource.isReadyForSchedule()).isTrue();
    }
}
