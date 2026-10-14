package com.maruthi.orders.dto;

import com.maruthi.orders.model.Order;
import com.maruthi.orders.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderResponse(
        Long id,
        String customerId,
        String product,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant createdAt,
        Instant updatedAt
) {
    public static OrderResponse from(Order order) {
        return new OrderResponse(
                order.getId(),
                order.getCustomerId(),
                order.getProduct(),
                order.getQuantity(),
                order.getUnitPrice(),
                order.totalAmount(),
                order.getStatus(),
                order.getCreatedAt(),
                order.getUpdatedAt()
        );
    }
}
