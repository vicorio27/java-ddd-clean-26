package com.sandbox.app;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.web.client.RestClient;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

/**
 * Recorre create -> outbox -> relay -> Kafka -> pay, contra Postgres y Kafka reales.
 *
 * <p>Es la prueba que faltaba: el repo afirmaba que la infraestructura es intercambiable
 * sin que ningun test ejecutara nunca un adaptador.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class OrderLifecycleIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:17");

    @Container
    static final KafkaContainer KAFKA = new KafkaContainer(DockerImageName.parse("apache/kafka:3.9.0"));

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @LocalServerPort
    int port;

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Autowired
    ObjectMapper objectMapper;

    RestClient client;

    @BeforeEach
    void setUp() {
        client = RestClient.create("http://localhost:" + port);
    }

    @Test
    void createsOrderStoresEventInOutboxAndRelaysItToKafka() {
        var customerId = insertCustomer();

        var created = client.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("customerId", customerId.toString(),
                        "lines", List.of(Map.of("productId", "SKU-1", "quantity", 2,
                                "unitPrice", new BigDecimal("10.00"), "currency", "USD"))))
                .retrieve()
                .body(Map.class);

        var orderId = (String) created.get("orderId");
        assertThat(created.get("status")).isEqualTo("CREATED");

        // El evento se escribio en el mismo commit que la orden, no en Kafka.
        var outboxRows = jdbcTemplate.queryForList(
                "SELECT event_type, published FROM outbox_events WHERE aggregate_type = 'Order'");
        assertThat(outboxRows).hasSize(1);
        assertThat(outboxRows.getFirst().get("event_type")).isEqualTo("OrderCreatedEvent");

        // Y el relay lo publica de forma asincrona, marcandolo solo tras la confirmacion del broker.
        await().atMost(Duration.ofSeconds(30)).untilAsserted(() ->
                assertThat(jdbcTemplate.queryForObject(
                        "SELECT count(*) FROM outbox_events WHERE published = TRUE", Integer.class))
                        .isEqualTo(1));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?::uuid", String.class, orderId))
                .isEqualTo("CREATED");
    }

    @Test
    void repeatingAPaymentWithTheSameIdempotencyKeyDoesNotChargeTwice() {
        var customerId = insertCustomer();
        var orderId = createOrder(customerId);
        var idempotencyKey = UUID.randomUUID().toString();

        var first = pay(orderId, idempotencyKey);
        var second = pay(orderId, idempotencyKey);

        assertThat(first.get("status")).isEqualTo("COMPLETED");
        assertThat(second.get("paymentId")).isEqualTo(first.get("paymentId"));

        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE order_reference = ?", Integer.class, orderId))
                .isEqualTo(1);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT status FROM orders WHERE id = ?::uuid", String.class, orderId))
                .isEqualTo("PAID");
    }

    @Test
    void aPaymentRetryWithADifferentKeyIsRejectedByTheOrderStateMachine() {
        var customerId = insertCustomer();
        var orderId = createOrder(customerId);

        pay(orderId, UUID.randomUUID().toString());

        var response = client.post()
                .uri("/api/v1/orders/{id}/payments", orderId)
                .header("Idempotency-Key", UUID.randomUUID().toString())
                .retrieve()
                .onStatus(status -> true, (req, res) -> { })
                .toBodilessEntity();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
        assertThat(jdbcTemplate.queryForObject(
                "SELECT count(*) FROM payments WHERE order_reference = ?", Integer.class, orderId))
                .isEqualTo(2);
    }

    @Test
    void updatingAnOrderPreservesCreatedAtAndBumpsTheOptimisticLockVersion() {
        var customerId = insertCustomer();
        var orderId = createOrder(customerId);

        var createdAtBefore = jdbcTemplate.queryForObject(
                "SELECT created_at FROM orders WHERE id = ?::uuid", java.sql.Timestamp.class, orderId);
        var versionBefore = jdbcTemplate.queryForObject(
                "SELECT version FROM orders WHERE id = ?::uuid", Long.class, orderId);

        pay(orderId, UUID.randomUUID().toString());

        var createdAtAfter = jdbcTemplate.queryForObject(
                "SELECT created_at FROM orders WHERE id = ?::uuid", java.sql.Timestamp.class, orderId);
        var versionAfter = jdbcTemplate.queryForObject(
                "SELECT version FROM orders WHERE id = ?::uuid", Long.class, orderId);

        assertThat(createdAtAfter).isEqualTo(createdAtBefore);
        assertThat(versionAfter).isGreaterThan(versionBefore);
    }

    private UUID insertCustomer() {
        var id = UUID.randomUUID();
        jdbcTemplate.update("INSERT INTO customers (id, name, email, status) VALUES (?, ?, ?, ?)",
                id, "Ada Lovelace", id + "@sandbox.local", "ACTIVE");
        return id;
    }

    private String createOrder(UUID customerId) {
        var created = client.post()
                .uri("/api/v1/orders")
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("customerId", customerId.toString(),
                        "lines", List.of(Map.of("productId", "SKU-1", "quantity", 2,
                                "unitPrice", new BigDecimal("10.00"), "currency", "USD"))))
                .retrieve()
                .body(Map.class);
        return (String) created.get("orderId");
    }

    private Map<?, ?> pay(String orderId, String idempotencyKey) {
        return client.post()
                .uri("/api/v1/orders/{id}/payments", orderId)
                .header("Idempotency-Key", idempotencyKey)
                .retrieve()
                .body(Map.class);
    }
}
