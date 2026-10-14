package com.maruthi.orders.dto;

import com.maruthi.orders.model.Order;
import com.maruthi.orders.model.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Event published to the {@code order-events} Kafka topic whenever an
 * order is created or its status changes.
 */
public record OrderEvent(
        String eventType,
        Long orderId,
        String customerId,
        String product,
        int quantity,
        BigDecimal totalAmount,
        OrderStatus status,
        Instant occurredAt
) {
    public static OrderEvent of(String eventType, Order order) {
        return new OrderEvent(
                eventType,
                order.getId(),
                order.getCustomerId(),
                order.getProduct(),
                order.getQuantity(),
                order.totalAmount(),
                order.getStatus(),
                Instant.now()
        );
    }
}
