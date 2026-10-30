package com.maruthi.orders;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.maruthi.orders.dto.OrderEvent;
import com.maruthi.orders.kafka.OrderEventListener;
import com.maruthi.orders.model.OrderStatus;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for the fulfilment consumer. The listener's behaviour is
 * its per-event-type handling, which today is fulfilment logging, so
 * the tests capture the log output and assert the right branch ran
 * with the event's data.
 */
class OrderEventListenerTest {

    private final OrderEventListener listener = new OrderEventListener();

    private Logger listenerLogger;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        listenerLogger = (Logger) LoggerFactory.getLogger(OrderEventListener.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        listenerLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        listenerLogger.detachAppender(logAppender);
    }

    @Test
    void onOrderEvent_createdEvent_reservesInventory() {
        OrderEvent event = new OrderEvent(
                "ORDER_CREATED", 42L, "cust-1", "Mechanical Keyboard", 2,
                new BigDecimal("159.98"), OrderStatus.CREATED, Instant.now());

        listener.onOrderEvent(event);

        assertThat(logMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("New order 42")
                        .contains("cust-1")
                        .contains("reserving inventory"));
    }

    @Test
    void onOrderEvent_statusChangedEvent_notifiesCustomer() {
        OrderEvent event = new OrderEvent(
                "ORDER_STATUS_CHANGED", 42L, "cust-1", "Mechanical Keyboard", 2,
                new BigDecimal("159.98"), OrderStatus.CONFIRMED, Instant.now());

        listener.onOrderEvent(event);

        assertThat(logMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("Order 42 moved to status CONFIRMED")
                        .contains("notifying customer cust-1"));
    }

    @Test
    void onOrderEvent_unknownEventType_logsGenericReceipt() {
        OrderEvent event = new OrderEvent(
                "ORDER_REFUNDED", 7L, "cust-9", "Mouse", 1,
                new BigDecimal("25.00"), OrderStatus.CANCELLED, Instant.now());

        listener.onOrderEvent(event);

        assertThat(logMessages())
                .anySatisfy(message -> assertThat(message)
                        .contains("Received event ORDER_REFUNDED for order 7"));
    }

    private List<String> logMessages() {
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }
}
