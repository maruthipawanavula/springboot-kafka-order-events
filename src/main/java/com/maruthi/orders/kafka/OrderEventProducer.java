package com.maruthi.orders.kafka;

import com.maruthi.orders.dto.OrderEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

/**
 * Publishes order lifecycle events to Kafka.
 *
 * Events are keyed by order id so all events for one order land on the
 * same partition and are consumed in order.
 */
@Component
public class OrderEventProducer {

    private static final Logger log = LoggerFactory.getLogger(OrderEventProducer.class);

    private final KafkaTemplate<String, OrderEvent> kafkaTemplate;

    public OrderEventProducer(KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void publish(OrderEvent event) {
        String key = String.valueOf(event.orderId());
        kafkaTemplate.send(KafkaTopicConfig.ORDER_EVENTS_TOPIC, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish event {} for order {}", event.eventType(), event.orderId(), ex);
                    } else {
                        log.info("Published {} for order {} to partition {} offset {}",
                                event.eventType(), event.orderId(),
                                result.getRecordMetadata().partition(),
                                result.getRecordMetadata().offset());
                    }
                });
    }
}
