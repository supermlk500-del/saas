package com.zhihuitong.modules.order.controller;

import com.zhihuitong.common.domain.AjaxResult;
import com.zhihuitong.common.domain.TableDataInfo;
import com.zhihuitong.modules.order.dto.OrderBatchLinkUpsertRequest;
import com.zhihuitong.modules.order.dto.OrderItemUpsertRequest;
import com.zhihuitong.modules.order.dto.OrderQuery;
import com.zhihuitong.modules.order.dto.OrderStatusPatchRequest;
import com.zhihuitong.modules.order.dto.OrderUpsertRequest;
import com.zhihuitong.modules.order.entity.OrderBatchLink;
import com.zhihuitong.modules.order.entity.OrderInfo;
import com.zhihuitong.modules.order.entity.OrderItem;
import com.zhihuitong.modules.order.service.OrderService;
import com.zhihuitong.modules.order.vo.OrderBatchLinkVo;
import com.zhihuitong.modules.order.vo.OrderDetailVo;
import com.zhihuitong.modules.order.vo.OrderExceptionSummaryVo;
import com.zhihuitong.modules.order.vo.OrderItemVo;
import com.zhihuitong.modules.order.vo.OrderListVo;
import com.zhihuitong.modules.order.vo.OrderMachineSummaryVo;
import com.zhihuitong.modules.order.vo.OrderPlanSummaryVo;
import com.zhihuitong.modules.order.vo.OrderQcSummaryVo;
import com.zhihuitong.modules.order.vo.OrderRouteSummaryVo;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@Validated
@RestController
@PreAuthorize("@auth.hasPermission('order:order:list')")
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public TableDataInfo<OrderListVo> list(@Valid @ModelAttribute OrderQuery query) {
        return orderService.list(query);
    }

    @GetMapping("/{orderId}")
    public AjaxResult detail(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId) {
        OrderDetailVo detail = orderService.getDetail(orderId);
        return AjaxResult.success(detail);
    }

    @PreAuthorize("@auth.hasPermission('order:order:add')")
    @PostMapping
    public AjaxResult create(@Valid @RequestBody OrderUpsertRequest request) {
        OrderInfo order = orderService.create(request);
        return AjaxResult.success(order);
    }

    @PreAuthorize("@auth.hasPermission('order:order:edit')")
    @PutMapping("/{orderId}")
    public AjaxResult update(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId,
                             @Valid @RequestBody OrderUpsertRequest request) {
        OrderInfo order = orderService.update(orderId, request);
        return AjaxResult.success(order);
    }

    @PreAuthorize("@auth.hasPermission('order:order:remove')")
    @DeleteMapping("/{orderId}")
    public AjaxResult delete(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId) {
        orderService.delete(orderId);
        return AjaxResult.success();
    }

    @PreAuthorize("@auth.hasPermission('order:order:edit')")
    @PatchMapping("/{orderId}/status")
    public AjaxResult patchStatus(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId,
                                  @Valid @RequestBody OrderStatusPatchRequest request) {
        OrderInfo order = orderService.patchStatus(orderId, request);
        return AjaxResult.success(order);
    }

    @GetMapping("/{orderId}/items")
    public AjaxResult listItems(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId) {
        List<OrderItemVo> items = orderService.listItems(orderId);
        return AjaxResult.success(items);
    }

    @PreAuthorize("@auth.hasPermission('order:order:add')")
    @PostMapping("/{orderId}/items")
    public AjaxResult createItem(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId,
                                 @Valid @RequestBody OrderItemUpsertRequest request) {
        OrderItem item = orderService.createItem(orderId, request);
        return AjaxResult.success(item);
    }

    @PreAuthorize("@auth.hasPermission('order:order:edit')")
    @PutMapping("/{orderId}/items/{orderItemId}")
    public AjaxResult updateItem(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId,
                                 @PathVariable @Min(value = 1, message = "orderItemId must be greater than 0") Long orderItemId,
                                 @Valid @RequestBody OrderItemUpsertRequest request) {
        OrderItem item = orderService.updateItem(orderId, orderItemId, request);
        return AjaxResult.success(item);
    }

    @PreAuthorize("@auth.hasPermission('order:order:remove')")
    @DeleteMapping("/{orderId}/items/{orderItemId}")
    public AjaxResult deleteItem(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId,
                                 @PathVariable @Min(value = 1, message = "orderItemId must be greater than 0") Long orderItemId) {
        orderService.deleteItem(orderId, orderItemId);
        return AjaxResult.success();
    }

    @GetMapping("/{orderId}/batches")
    public AjaxResult listBatches(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId) {
        List<OrderBatchLinkVo> rows = orderService.listOrderBatches(orderId);
        return AjaxResult.success(rows);
    }

    @GetMapping("/{orderId}/plans")
    public AjaxResult listPlans(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId) {
        List<OrderPlanSummaryVo> rows = orderService.listOrderPlans(orderId);
        return AjaxResult.success(rows);
    }

    @GetMapping("/{orderId}/routes")
    public AjaxResult listRoutes(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId) {
        List<OrderRouteSummaryVo> rows = orderService.listOrderRoutes(orderId);
        return AjaxResult.success(rows);
    }

    @GetMapping("/{orderId}/machines")
    public AjaxResult listMachines(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId) {
        List<OrderMachineSummaryVo> rows = orderService.listOrderMachines(orderId);
        return AjaxResult.success(rows);
    }

    @GetMapping("/{orderId}/qc-records")
    public AjaxResult listQcRecords(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId) {
        List<OrderQcSummaryVo> rows = orderService.listOrderQcRecords(orderId);
        return AjaxResult.success(rows);
    }

    @GetMapping("/{orderId}/exceptions")
    public AjaxResult listExceptions(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId) {
        List<OrderExceptionSummaryVo> rows = orderService.listOrderExceptions(orderId);
        return AjaxResult.success(rows);
    }

    @PreAuthorize("@auth.hasPermission('order:order:add')")
    @PostMapping("/{orderId}/batches")
    public AjaxResult createBatchLink(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId,
                                      @Valid @RequestBody OrderBatchLinkUpsertRequest request) {
        OrderBatchLink link = orderService.createBatchLink(orderId, request);
        return AjaxResult.success(link);
    }

    @PreAuthorize("@auth.hasPermission('order:order:edit')")
    @PutMapping("/{orderId}/batches/{linkId}")
    public AjaxResult updateBatchLink(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId,
                                      @PathVariable @Min(value = 1, message = "linkId must be greater than 0") Long linkId,
                                      @Valid @RequestBody OrderBatchLinkUpsertRequest request) {
        OrderBatchLink link = orderService.updateBatchLink(orderId, linkId, request);
        return AjaxResult.success(link);
    }

    @PreAuthorize("@auth.hasPermission('order:order:remove')")
    @DeleteMapping("/{orderId}/batches/{linkId}")
    public AjaxResult deleteBatchLink(@PathVariable @Min(value = 1, message = "orderId must be greater than 0") Long orderId,
                                      @PathVariable @Min(value = 1, message = "linkId must be greater than 0") Long linkId) {
        orderService.deleteBatchLink(orderId, linkId);
        return AjaxResult.success();
    }
}
