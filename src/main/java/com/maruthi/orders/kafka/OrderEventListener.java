package com.maruthi.orders.kafka;

import com.maruthi.orders.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Consumer module for order events.
 *
 * In a real deployment this listener would live in its own deployable
 * service (e.g. a fulfilment or notification service) with its own
 * consumer group. It is kept in a separate package here to show the
 * producer/consumer split; the group id is what decouples them.
 */
@Component
public class OrderEventListener {

    private static final Logger log = LoggerFactory.getLogger(OrderEventListener.class);

    @KafkaListener(
            topics = KafkaTopicConfig.ORDER_EVENTS_TOPIC,
            groupId = "${spring.kafka.consumer.group-id}"
    )
    public void onOrderEvent(OrderEvent event) {
        switch (event.eventType()) {
            case "ORDER_CREATED" ->
                    log.info("[fulfilment] New order {} for customer {}: {} x {} (total {}) — reserving inventory",
                            event.orderId(), event.customerId(), event.quantity(),
                            event.product(), event.totalAmount());
            case "ORDER_STATUS_CHANGED" ->
                    log.info("[fulfilment] Order {} moved to status {} — notifying customer {}",
                            event.orderId(), event.status(), event.customerId());
            default ->
                    log.info("[fulfilment] Received event {} for order {}", event.eventType(), event.orderId());
        }
    }
}
