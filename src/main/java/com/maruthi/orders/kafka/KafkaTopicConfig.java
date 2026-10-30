package com.maruthi.orders.kafka;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    public static final String ORDER_EVENTS_TOPIC = "order-events";

    /** Dead-letter topic for records that fail processing after retries. */
    public static final String ORDER_EVENTS_DLT_TOPIC = ORDER_EVENTS_TOPIC + ".DLT";

    @Value("${app.kafka.order-events.partitions:3}")
    private int partitions;

    @Value("${app.kafka.order-events.replicas:1}")
    private int replicas;

    @Bean
    public NewTopic orderEventsTopic() {
        return TopicBuilder.name(ORDER_EVENTS_TOPIC)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }

    @Bean
    public NewTopic orderEventsDltTopic() {
        return TopicBuilder.name(ORDER_EVENTS_DLT_TOPIC)
                .partitions(partitions)
                .replicas(replicas)
                .build();
    }
}
