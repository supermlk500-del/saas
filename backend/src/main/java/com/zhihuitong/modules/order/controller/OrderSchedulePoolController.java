package com.zhihuitong.modules.order.controller;

import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.order.dto.OrderSchedulePoolQuery;
import com.zhihuitong.modules.order.service.OrderService;
import com.zhihuitong.modules.order.vo.OrderSchedulePoolVo;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@PreAuthorize("@auth.hasPermission('plan:production:list')")
@RequestMapping("/api/order-schedule-pool")
public class OrderSchedulePoolController {

    private final OrderService orderService;

    public OrderSchedulePoolController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public TableDataInfo<OrderSchedulePoolVo> list(@Valid @ModelAttribute OrderSchedulePoolQuery query) {
        return orderService.listSchedulePool(query);
    }
}
