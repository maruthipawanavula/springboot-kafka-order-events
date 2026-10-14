package com.maruthi.orders.service;

import com.maruthi.orders.dto.CreateOrderRequest;
import com.maruthi.orders.dto.OrderEvent;
import com.maruthi.orders.exception.ResourceNotFoundException;
import com.maruthi.orders.kafka.OrderEventProducer;
import com.maruthi.orders.model.Order;
import com.maruthi.orders.model.OrderStatus;
import com.maruthi.orders.repository.OrderRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderEventProducer eventProducer;

    public OrderService(OrderRepository orderRepository, OrderEventProducer eventProducer) {
        this.orderRepository = orderRepository;
        this.eventProducer = eventProducer;
    }

    public Order createOrder(CreateOrderRequest request) {
        Order order = new Order(
                request.customerId(),
                request.product(),
                request.quantity(),
                request.unitPrice()
        );
        Order saved = orderRepository.save(order);
        eventProducer.publish(OrderEvent.of("ORDER_CREATED", saved));
        return saved;
    }

    @Transactional(readOnly = true)
    public Order getOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found: " + id));
    }

    @Transactional(readOnly = true)
    public List<Order> listOrders(String customerId) {
        if (customerId != null && !customerId.isBlank()) {
            return orderRepository.findByCustomerId(customerId);
        }
        return orderRepository.findAll();
    }

    public Order updateStatus(Long id, OrderStatus newStatus) {
        Order order = getOrder(id);
        if (order.getStatus() == OrderStatus.CANCELLED || order.getStatus() == OrderStatus.SHIPPED) {
            throw new IllegalStateException(
                    "Order " + id + " is " + order.getStatus() + " and cannot change status");
        }
        order.setStatus(newStatus);
        Order saved = orderRepository.save(order);
        eventProducer.publish(OrderEvent.of("ORDER_STATUS_CHANGED", saved));
        return saved;
    }
}
