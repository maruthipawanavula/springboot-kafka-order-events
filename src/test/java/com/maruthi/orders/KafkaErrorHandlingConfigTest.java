package com.maruthi.orders;

import com.maruthi.orders.dto.OrderEvent;
import com.maruthi.orders.kafka.KafkaErrorHandlingConfig;
import com.maruthi.orders.kafka.KafkaTopicConfig;
import com.maruthi.orders.model.OrderStatus;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.listener.DeadLetterPublishingRecoverer;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class KafkaErrorHandlingConfigTest {

    @Mock
    private KafkaTemplate<String, OrderEvent> kafkaTemplate;

    @Test
    @SuppressWarnings({"unchecked", "rawtypes"})
    void recover_publishesFailedRecordToDeadLetterTopic() {
        when(kafkaTemplate.send(any(ProducerRecord.class)))
                .thenReturn(CompletableFuture.completedFuture(null));

        KafkaErrorHandlingConfig config = new KafkaErrorHandlingConfig();
        DeadLetterPublishingRecoverer recoverer =
                config.deadLetterPublishingRecoverer(kafkaTemplate);

        OrderEvent event = new OrderEvent(
                "ORDER_CREATED", 42L, "cust-1", "Mechanical Keyboard", 2,
                new BigDecimal("159.98"), OrderStatus.CREATED, Instant.now());
        ConsumerRecord<String, OrderEvent> failedRecord = new ConsumerRecord<>(
                KafkaTopicConfig.ORDER_EVENTS_TOPIC, 1, 7L, "42", event);

        recoverer.accept(failedRecord, null, new IllegalStateException("fulfilment failed"));

        ArgumentCaptor<ProducerRecord> captor = ArgumentCaptor.forClass(ProducerRecord.class);
        verify(kafkaTemplate).send(captor.capture());
        ProducerRecord<String, OrderEvent> deadLetter = captor.getValue();
        assertThat(deadLetter.topic()).isEqualTo(KafkaTopicConfig.ORDER_EVENTS_DLT_TOPIC);
        assertThat(deadLetter.partition()).isEqualTo(1);
        assertThat(deadLetter.key()).isEqualTo("42");
        assertThat(deadLetter.value()).isEqualTo(event);
    }

    @Test
    void kafkaErrorHandler_isDefaultErrorHandler() {
        KafkaErrorHandlingConfig config = new KafkaErrorHandlingConfig();
        DeadLetterPublishingRecoverer recoverer =
                config.deadLetterPublishingRecoverer(kafkaTemplate);

        assertThat(config.kafkaErrorHandler(recoverer))
                .isInstanceOf(org.springframework.kafka.listener.DefaultErrorHandler.class);
    }
}
