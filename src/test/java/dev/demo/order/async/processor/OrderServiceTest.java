package dev.demo.order.async.processor;

import dev.demo.order.async.processor.repository.OrderRepository;
import dev.demo.order.async.processor.repository.model.Order;
import dev.demo.order.async.processor.service.OrderService;
import dev.demo.order.async.processor.service.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        orderService = new OrderServiceImpl(orderRepository);

        // Set properties using reflection
        ReflectionTestUtils.setField(orderService, "pendingStatus", "PENDING");
        ReflectionTestUtils.setField(orderService, "processingStatus", "PROCESSING");
        ReflectionTestUtils.setField(orderService, "completedStatus", "COMPLETED");
        ReflectionTestUtils.setField(orderService, "errorStatus", "ERROR");
        ReflectionTestUtils.setField(orderService, "maxAge", java.time.Duration.ofHours(24));
    }

    @Test
    void findOrdersToProcess_ShouldReturnOrders() {
        // Arrange
        Order order1 = new Order();
        order1.setOrderId("order-1");
        order1.setStatus("PENDING");

        Order order2 = new Order();
        order2.setOrderId("order-2");
        order2.setStatus("PENDING");

        when(orderRepository.findOrdersToProcess(
                eq(List.of("PENDING")),
                any(LocalDateTime.class),
                anyInt(),
                anyLong()))
                .thenReturn(Flux.just(order1, order2));

        // Act & Assert
        StepVerifier.create(orderService.findOrdersToProcess(10))
                .expectNext(order1)
                .expectNext(order2)
                .verifyComplete();
    }

    @Test
    void findOrdersToProcessByTypes_ShouldReturnOrdersOfSpecifiedTypes() {
        // Arrange
        Order order1 = new Order();
        order1.setOrderId("order-1");
        order1.setStatus("PENDING");
        order1.setType("STANDARD");

        when(orderRepository.findOrdersToProcessByTypes(
                eq(List.of("PENDING")),
                any(LocalDateTime.class),
                eq(List.of("STANDARD", "PRIORITY")),
                anyInt()))
                .thenReturn(Flux.just(order1));

        // Act & Assert
        StepVerifier.create(orderService.findOrdersToProcessByTypes(List.of("STANDARD", "PRIORITY"), 10))
                .expectNext(order1)
                .verifyComplete();
    }

    @Test
    void updateOrderStatus_ShouldUpdateAndReturnOrder() {
        // Arrange
        Order order = new Order();
        order.setOrderId("order-1");
        order.setStatus("COMPLETED");

        when(orderRepository.updateOrderStatus(eq("order-1"), eq("COMPLETED"), any(LocalDateTime.class)))
                .thenReturn(Mono.just(1));

        when(orderRepository.findById("order-1"))
                .thenReturn(Mono.just(order));

        // Act & Assert
        StepVerifier.create(orderService.updateOrderStatus("order-1", "COMPLETED"))
                .expectNext(order)
                .verifyComplete();
    }
}