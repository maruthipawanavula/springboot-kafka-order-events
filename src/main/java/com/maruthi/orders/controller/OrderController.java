package com.maruthi.orders.controller;

import com.maruthi.orders.dto.CreateOrderRequest;
import com.maruthi.orders.dto.OrderResponse;
import com.maruthi.orders.model.OrderStatus;
import com.maruthi.orders.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody CreateOrderRequest request) {
        OrderResponse response = OrderResponse.from(orderService.createOrder(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return OrderResponse.from(orderService.getOrder(id));
    }

    @GetMapping
    public List<OrderResponse> listOrders(@RequestParam(required = false) String customerId) {
        return orderService.listOrders(customerId).stream()
                .map(OrderResponse::from)
                .toList();
    }

    @PatchMapping("/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @RequestBody Map<String, String> body) {
        String rawStatus = body.get("status");
        if (rawStatus == null || rawStatus.isBlank()) {
            throw new IllegalArgumentException("status is required");
        }
        OrderStatus status = OrderStatus.valueOf(rawStatus.toUpperCase());
        return OrderResponse.from(orderService.updateStatus(id, status));
    }
}
