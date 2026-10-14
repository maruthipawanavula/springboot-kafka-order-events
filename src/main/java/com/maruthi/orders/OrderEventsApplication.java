package com.maruthi.orders;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Entry point for the order events service.
 *
 * The application exposes a REST API for orders, persists them with JPA,
 * publishes lifecycle events to Kafka, and runs a Kafka listener module
 * that reacts to those events (logging / fulfilment simulation).
 */
@SpringBootApplication
public class OrderEventsApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderEventsApplication.class, args);
    }
}
