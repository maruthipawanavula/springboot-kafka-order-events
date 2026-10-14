package com.maruthi.orders;

import com.maruthi.orders.dto.CreateOrderRequest;
import com.maruthi.orders.dto.OrderEvent;
import com.maruthi.orders.exception.ResourceNotFoundException;
import com.maruthi.orders.kafka.OrderEventProducer;
import com.maruthi.orders.model.Order;
import com.maruthi.orders.model.OrderStatus;
import com.maruthi.orders.repository.OrderRepository;
import com.maruthi.orders.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderEventProducer eventProducer;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderService(orderRepository, eventProducer);
    }

    @Test
    void createOrder_savesOrderAndPublishesCreatedEvent() {
        CreateOrderRequest request = new CreateOrderRequest(
                "cust-1", "Mechanical Keyboard", 2, new BigDecimal("79.99"));

        when(orderRepository.save(any(Order.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        Order created = orderService.createOrder(request);

        assertThat(created.getStatus()).isEqualTo(OrderStatus.CREATED);
        assertThat(created.totalAmount()).isEqualByComparingTo(new BigDecimal("159.98"));

        ArgumentCaptor<OrderEvent> captor = ArgumentCaptor.forClass(OrderEvent.class);
        verify(eventProducer).publish(captor.capture());
        assertThat(captor.getValue().eventType()).isEqualTo("ORDER_CREATED");
        assertThat(captor.getValue().customerId()).isEqualTo("cust-1");
    }

    @Test
    void getOrder_throwsWhenMissing() {
        when(orderRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderService.getOrder(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void updateStatus_rejectsChangeForShippedOrder() {
        Order shipped = new Order("cust-1", "Mouse", 1, new BigDecimal("25.00"));
        shipped.setStatus(OrderStatus.SHIPPED);
        when(orderRepository.findById(1L)).thenReturn(Optional.of(shipped));

        assertThatThrownBy(() -> orderService.updateStatus(1L, OrderStatus.CANCELLED))
                .isInstanceOf(IllegalStateException.class);
    }
}
