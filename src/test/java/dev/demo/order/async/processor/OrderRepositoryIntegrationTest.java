package dev.demo.order.async.processor;

import dev.demo.order.async.processor.repository.OrderRepository;
import dev.demo.order.async.processor.repository.model.Customer;
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
        Customer customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setExternalId(UUID.randomUUID().toString());
        customer.setClientItn("123456789");
        customer.setClientName("Test Customer");
        customer.setClientSegment("REGULAR");

        Order order1 = new Order();
        order1.setOrderId(UUID.randomUUID().toString());
        order1.setType("STANDARD");
        order1.setCreatedAt(LocalDateTime.now().minusHours(2));
        order1.setCreator("test-user");
        order1.setBranch("test-branch");
        order1.setClient(customer);
        order1.setStatus("PENDING");

        Order order2 = new Order();
        order2.setOrderId(UUID.randomUUID().toString());
        order2.setType("PRIORITY");
        order2.setCreatedAt(LocalDateTime.now().minusHours(1));
        order2.setCreator("test-user");
        order2.setBranch("test-branch");
        order2.setClient(customer);
        order2.setStatus("PENDING");

        Order order3 = new Order();
        order3.setOrderId(UUID.randomUUID().toString());
        order3.setType("STANDARD");
        order3.setCreatedAt(LocalDateTime.now().minusHours(3));
        order3.setCreator("test-user");
        order3.setBranch("test-branch");
        order3.setClient(customer);
        order3.setStatus("COMPLETED");

        orderRepository.saveAll(List.of(order1, order2, order3)).blockLast();

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
        Customer customer = new Customer();
        customer.setAbsClientId(1);
        customer.setEkbClientId(2);
        customer.setClientItn("123456789");
        customer.setClientName("Test Customer");
        customer.setClientSegment("REGULAR");

        Order order1 = new Order();
        order1.setOrderId(UUID.randomUUID().toString());
        order1.setType("STANDARD");
        order1.setCreatedAt(LocalDateTime.now().minusHours(2));
        order1.setCreator("test-user");
        order1.setBranch("test-branch");
        order1.setClient(customer);
        order1.setStatus("PENDING");

        Order order2 = new Order();
        order2.setOrderId(UUID.randomUUID().toString());
        order2.setType("PRIORITY");
        order2.setCreatedAt(LocalDateTime.now().minusHours(1));
        order2.setCreator("test-user");
        order2.setBranch("test-branch");
        order2.setClient(customer);
        order2.setStatus("PENDING");

        orderRepository.saveAll(List.of(order1, order2)).blockLast();

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
        Customer customer = new Customer();
        customer.setAbsClientId(1);
        customer.setEkbClientId(2);
        customer.setClientItn("123456789");
        customer.setClientName("Test Customer");
        customer.setClientSegment("REGULAR");

        Order order = new Order();
        order.setOrderId(UUID.randomUUID().toString());
        order.setType("STANDARD");
        order.setCreatedAt(LocalDateTime.now().minusHours(2));
        order.setCreator("test-user");
        order.setBranch("test-branch");
        order.setClient(customer);
        order.setStatus("PENDING");

        Order savedOrder = orderRepository.save(order).block();

        // Act & Assert
        LocalDateTime updateTime = LocalDateTime.now();
        StepVerifier.create(orderRepository.updateOrderStatus(savedOrder.getOrderId(), "COMPLETED", updateTime))
                .expectNext(1)
                .verifyComplete();

        StepVerifier.create(orderRepository.findById(savedOrder.getOrderId()))
                .expectNextMatches(o -> "COMPLETED".equals(o.getStatus()))
                .verifyComplete();
    }
}