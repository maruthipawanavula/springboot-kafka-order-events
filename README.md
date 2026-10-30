# Spring Boot + Kafka Order Events

An event-driven order microservice built with **Java 17, Spring Boot 3, Spring Kafka, and Spring Data JPA**. Orders are managed through a REST API; every order creation and status change is published as an event to a Kafka topic and processed by a separate consumer module (a fulfilment listener) — the classic producer/consumer split used in real microservice systems.

By Maruthi Pawan Avula.

## Architecture

```
                 ┌──────────────────────┐
   HTTP client ──▶  OrderController   │
                 │  (REST API)          │
                 └─────────┬────────────┘
                           │
                 ┌─────────▼────────────┐        ┌──────────────────┐
                 │    OrderService      │───────▶│   PostgreSQL /   │
                 │  (business logic)    │        │   H2 (JPA)       │
                 └─────────┬────────────┘        └──────────────────┘
                           │ publishes OrderEvent (key = orderId)
                 ┌─────────▼────────────┐
                 │ OrderEventProducer   │
                 └─────────┬────────────┘
                           │
                 ┌─────────▼────────────┐
                 │  Kafka topic:        │
                 │  order-events (3     │
                 │  partitions)         │
                 └─────────┬────────────┘
                           │ consumed by group
                           │ "order-fulfilment-group"
                 ┌─────────▼────────────┐
                 │ OrderEventListener   │
                 │ (fulfilment module — │
                 │ inventory / notify)  │
                 └──────────────────────┘
```

**Design notes**

- Events are keyed by `orderId`, so all events for one order land on the same partition and are consumed in order.
- The producer uses `acks=all` and idempotence; event publishing is handled in the service layer right after the JPA save.
- The listener lives in its own package with its own consumer group — in production this would be a separately deployed fulfilment/notification service reading the same topic.
- Errors are returned as RFC 7807 `ProblemDetail` responses (404 for missing orders, 400 for validation failures, 409 for illegal status transitions).

## Tech stack

- Java 17, Spring Boot 3.3 (Web, Data JPA, Validation, Actuator)
- Spring Kafka 3.2 (JSON serialization)
- PostgreSQL 16 (Docker) / H2 in-memory (local default profile)
- Maven, Docker, Docker Compose
- JUnit 5, Mockito, MockMvc tests

## How to run

### Option 1 — Docker Compose (full stack: app + Kafka + Postgres)

```bash
docker compose up --build
```

The API is then available at http://localhost:8080.

### Option 2 — Local (H2 + a Kafka broker)

Start Kafka (for example with the `kafka` and `zookeeper` services only):

```bash
docker compose up kafka zookeeper
```

Then run the app with the default profile (in-memory H2 database):

```bash
mvn spring-boot:run
```

## API endpoints

| Method | Path                      | Description                                  |
|--------|---------------------------|----------------------------------------------|
| POST   | `/api/orders`             | Create an order (publishes `ORDER_CREATED`)  |
| GET    | `/api/orders/{id}`        | Get one order                                |
| GET    | `/api/orders`             | List orders (optional `?customerId=` filter)|
| PATCH  | `/api/orders/{id}/status` | Change status (publishes `ORDER_STATUS_CHANGED`) |
| GET    | `/actuator/health`        | Health check                                 |

### Example

```bash
# Create an order
curl -X POST http://localhost:8080/api/orders \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"cust-1","product":"Mechanical Keyboard","quantity":2,"unitPrice":79.99}'

# Change its status
curl -X PATCH http://localhost:8080/api/orders/1/status \
  -H 'Content-Type: application/json' \
  -d '{"status":"CONFIRMED"}'

# Watch the events on the topic
docker compose exec kafka kafka-console-consumer \
  --bootstrap-server localhost:9092 --topic order-events --from-beginning
```

## Event flow

1. `POST /api/orders` validates the request, saves the order via JPA with status `CREATED`.
2. `OrderService` builds an `OrderEvent` (`eventType=ORDER_CREATED`) and hands it to `OrderEventProducer`.
3. The producer sends it to the `order-events` topic with the order id as the message key.
4. `OrderEventListener` (consumer group `order-fulfilment-group`) receives the event and runs fulfilment logic — here, structured logging that simulates inventory reservation and customer notification.
5. Status changes via `PATCH` repeat the same flow with `eventType=ORDER_STATUS_CHANGED`. Terminal states (`SHIPPED`, `CANCELLED`) reject further transitions with HTTP 409.

## Tests

[![CI](https://github.com/maruthipawanavula/springboot-kafka-order-events/actions/workflows/ci.yml/badge.svg)](https://github.com/maruthipawanavula/springboot-kafka-order-events/actions/workflows/ci.yml)

Every push and pull request is built and tested by the GitHub Actions CI workflow (`.github/workflows/ci.yml`, JDK 17, `mvn -B verify`). The suite is unit and MockMvc slice tests only, so it runs without Kafka or Postgres services.

```bash
mvn test
```

- `OrderServiceTest` — unit tests for order creation + event publishing, not-found handling, and status-transition guards (Mockito).
- `OrderControllerTest` — MockMvc tests for the REST layer (201 on create, 400 on invalid payloads).

## Project structure

```
src/main/java/com/maruthi/orders
├── controller/     REST endpoints
├── service/        business logic + event publishing
├── kafka/          topic config, producer, consumer listener
├── model/          JPA entity + status enum
├── dto/            request/response records + OrderEvent
├── repository/     Spring Data JPA repository
└── exception/      ProblemDetail exception handling
```
