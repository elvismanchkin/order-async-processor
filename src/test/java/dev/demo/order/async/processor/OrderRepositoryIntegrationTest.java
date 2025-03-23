package dev.demo.order.async.processor;

import dev.demo.order.async.processor.repository.OrderRepository;
import dev.demo.order.async.processor.repository.model.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.r2dbc.DataR2dbcTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import reactor.core.publisher.Flux;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@DataR2dbcTest
@Testcontainers
@ActiveProfiles("test")
class OrderRepositoryIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine")
            .withDatabaseName("testdb")
            .withUsername("test")
            .withPassword("test");

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.r2dbc.url", () -> String.format("r2dbc:postgresql://%s:%d/%s",
                postgres.getHost(), postgres.getFirstMappedPort(), postgres.getDatabaseName()));
        registry.add("spring.r2dbc.username", postgres::getUsername);
        registry.add("spring.r2dbc.password", postgres::getPassword);
    }

    @Autowired
    private OrderRepository orderRepository;

    @BeforeEach
    void setUp() {
        // Clean up any existing data
        orderRepository.deleteAll().block();
    }

    @Test
    void findOrdersToProcess_ShouldReturnMatchingOrders() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        Order order1 = new Order();
        order1.setId(UUID.randomUUID());
        order1.setReferenceNumber("ORD-001");
        order1.setType("STANDARD");
        order1.setCreatedAt(LocalDateTime.now().minusHours(2));
        order1.setCreatedBy("test-user");
        order1.setCustomerId(customerId);
        order1.setStatus("PENDING");
        order1.setDeleted(false);

        Order order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setReferenceNumber("ORD-002");
        order2.setType("PRIORITY");
        order2.setCreatedAt(LocalDateTime.now().minusHours(1));
        order2.setCreatedBy("test-user");
        order2.setCustomerId(customerId);
        order2.setStatus("PENDING");
        order2.setDeleted(false);

        Order order3 = new Order();
        order3.setId(UUID.randomUUID());
        order3.setReferenceNumber("ORD-003");
        order3.setType("STANDARD");
        order3.setCreatedAt(LocalDateTime.now().minusHours(3));
        order3.setCreatedBy("test-user");
        order3.setCustomerId(customerId);
        order3.setStatus("COMPLETED");
        order3.setDeleted(false);

        Flux.just(order1, order2, order3)
                .flatMap(orderRepository::save)
                .blockLast();

        // Act & Assert
        LocalDateTime cutoffTime = LocalDateTime.now();
        Flux<Order> result = orderRepository.findOrdersToProcess(
                List.of("PENDING"),
                cutoffTime,
                10,
                0);

        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();
    }

    @Test
    void findOrdersToProcessByTypes_ShouldReturnMatchingOrdersByType() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        Order order1 = new Order();
        order1.setId(UUID.randomUUID());
        order1.setReferenceNumber("ORD-001");
        order1.setType("STANDARD");
        order1.setCreatedAt(LocalDateTime.now().minusHours(2));
        order1.setCreatedBy("test-user");
        order1.setCustomerId(customerId);
        order1.setStatus("PENDING");
        order1.setDeleted(false);

        Order order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setReferenceNumber("ORD-002");
        order2.setType("PRIORITY");
        order2.setCreatedAt(LocalDateTime.now().minusHours(1));
        order2.setCreatedBy("test-user");
        order2.setCustomerId(customerId);
        order2.setStatus("PENDING");
        order2.setDeleted(false);

        Flux.just(order1, order2)
                .flatMap(orderRepository::save)
                .blockLast();

        // Act & Assert
        LocalDateTime cutoffTime = LocalDateTime.now();
        Flux<Order> result = orderRepository.findOrdersToProcessByTypes(
                List.of("PENDING"),
                cutoffTime,
                List.of("STANDARD"),
                10);

        StepVerifier.create(result)
                .expectNextMatches(order -> "STANDARD".equals(order.getType()))
                .verifyComplete();
    }

    @Test
    void updateOrderStatus_ShouldUpdateStatus() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setReferenceNumber("ORD-TEST");
        order.setType("STANDARD");
        order.setCreatedAt(LocalDateTime.now().minusHours(2));
        order.setCreatedBy("test-user");
        order.setCustomerId(customerId);
        order.setStatus("PENDING");
        order.setDeleted(false);

        Order savedOrder = orderRepository.save(order).block();

        // Act & Assert
        LocalDateTime updateTime = LocalDateTime.now();
        String updatedBy = "test-updater";

        StepVerifier.create(orderRepository.updateOrderStatus(savedOrder.getId(), "COMPLETED", updatedBy, updateTime))
                .expectNext(1)
                .verifyComplete();

        StepVerifier.create(orderRepository.findById(savedOrder.getId()))
                .expectNextMatches(o -> "COMPLETED".equals(o.getStatus()) && updatedBy.equals(o.getUpdatedBy()))
                .verifyComplete();
    }

    @Test
    void findByCustomerIdAndDeletedFalse_ShouldReturnCustomerOrders() {
        // Arrange
        UUID customerId1 = UUID.randomUUID();
        UUID customerId2 = UUID.randomUUID();

        Order order1 = new Order();
        order1.setId(UUID.randomUUID());
        order1.setReferenceNumber("ORD-C1-1");
        order1.setType("STANDARD");
        order1.setCreatedAt(LocalDateTime.now());
        order1.setCreatedBy("test-user");
        order1.setCustomerId(customerId1);
        order1.setStatus("PENDING");
        order1.setDeleted(false);

        Order order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setReferenceNumber("ORD-C1-2");
        order2.setType("PRIORITY");
        order2.setCreatedAt(LocalDateTime.now());
        order2.setCreatedBy("test-user");
        order2.setCustomerId(customerId1);
        order2.setStatus("COMPLETED");
        order2.setDeleted(false);

        Order order3 = new Order();
        order3.setId(UUID.randomUUID());
        order3.setReferenceNumber("ORD-C2-1");
        order3.setType("STANDARD");
        order3.setCreatedAt(LocalDateTime.now());
        order3.setCreatedBy("test-user");
        order3.setCustomerId(customerId2);
        order3.setStatus("PENDING");
        order3.setDeleted(false);

        Flux.just(order1, order2, order3)
                .flatMap(orderRepository::save)
                .blockLast();

        // Act & Assert
        StepVerifier.create(orderRepository.findByCustomerIdAndDeletedFalse(customerId1))
                .expectNextCount(2)
                .verifyComplete();

        StepVerifier.create(orderRepository.findByCustomerIdAndDeletedFalse(customerId2))
                .expectNextCount(1)
                .verifyComplete();
    }

    @Test
    void softDeleteOrder_ShouldMarkOrderAsDeleted() {
        // Arrange
        UUID customerId = UUID.randomUUID();

        Order order = new Order();
        order.setId(UUID.randomUUID());
        order.setReferenceNumber("ORD-DELETE");
        order.setType("STANDARD");
        order.setCreatedAt(LocalDateTime.now());
        order.setCreatedBy("test-user");
        order.setCustomerId(customerId);
        order.setStatus("PENDING");
        order.setDeleted(false);

        Order savedOrder = orderRepository.save(order).block();

        // Act & Assert
        LocalDateTime updateTime = LocalDateTime.now();
        String updatedBy = "test-deleter";

        StepVerifier.create(orderRepository.softDeleteOrder(savedOrder.getId(), updatedBy, updateTime))
                .expectNext(1)
                .verifyComplete();

        // Verify that the order is marked as deleted
        StepVerifier.create(orderRepository.findById(savedOrder.getId()))
                .expectNextMatches(o -> o.isDeleted() && updatedBy.equals(o.getUpdatedBy()))
                .verifyComplete();

        // Verify that the order is not returned by customer search
        StepVerifier.create(orderRepository.findByCustomerIdAndDeletedFalse(customerId))
                .expectNextCount(0)
                .verifyComplete();
    }
}