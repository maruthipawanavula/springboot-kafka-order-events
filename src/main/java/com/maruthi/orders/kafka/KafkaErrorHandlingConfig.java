package com.maruthi.orders.kafka;

import com.maruthi.orders.dto.OrderEvent;
import org.apache.kafka.common.TopicPartition;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.CommonErrorHandler;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;
import org.springframework.kafka.listener.DefaultErrorHandler;
import org.springframework.util.backoff.FixedBackOff;

/**
 * Consumer error handling for the order-events listener.
 *
 * A record that fails processing is retried twice (1 second apart);
 * if it still fails it is published — same key, same value, original
 * partition — to the dead-letter topic {@code order-events.DLT} by
 * the {@link DeadLetterPublishingRecoverer} instead of blocking the
 * partition or being silently dropped. The exception details travel
 * with the record as Kafka headers, so a failed event can be
 * inspected and replayed later.
 *
 * Spring Boot picks up the single {@link CommonErrorHandler} bean
 * and applies it to the default listener container factory.
 */
@Configuration
public class KafkaErrorHandlingConfig {

    @Bean
    public DeadLetterPublishingRecoverer deadLetterPublishingRecoverer(
            KafkaTemplate<String, OrderEvent> kafkaTemplate) {
        return new DeadLetterPublishingRecoverer(kafkaTemplate,
                (record, exception) -> new TopicPartition(
                        KafkaTopicConfig.ORDER_EVENTS_DLT_TOPIC, record.partition()));
    }

    @Bean
    public CommonErrorHandler kafkaErrorHandler(
            DeadLetterPublishingRecoverer deadLetterPublishingRecoverer) {
        DefaultErrorHandler errorHandler = new DefaultErrorHandler(
                deadLetterPublishingRecoverer, new FixedBackOff(1_000L, 2L));
        // Commit the offset of records that were recovered to the DLT
        // so the consumer moves past the poison record.
        errorHandler.setCommitRecovered(true);
        return errorHandler;
    }
}
