package dev.demo.order.async.processor;

import dev.demo.order.async.processor.repository.OrderActionRepository;
import dev.demo.order.async.processor.repository.OrderRepository;
import dev.demo.order.async.processor.repository.model.Order;
import dev.demo.order.async.processor.service.OrderService;
import dev.demo.order.async.processor.service.OrderServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderActionRepository actionRepository;

    private OrderService orderService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        orderService = new OrderServiceImpl(orderRepository, actionRepository);

        ReflectionTestUtils.setField(orderService, "pendingStatus", "PENDING");
        ReflectionTestUtils.setField(orderService, "processingStatus", "PROCESSING");
        ReflectionTestUtils.setField(orderService, "completedStatus", "COMPLETED");
        ReflectionTestUtils.setField(orderService, "errorStatus", "ERROR");
        ReflectionTestUtils.setField(orderService, "maxAge", Duration.ofHours(24));

        when(actionRepository.save(any())).thenReturn(Mono.empty());
    }

    @Test
    void findOrdersToProcess_ShouldReturnOrders() {
        Order order1 = new Order();
        order1.setId(UUID.randomUUID());
        order1.setStatus("PENDING");

        Order order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setStatus("PENDING");

        when(orderRepository.findOrdersToProcess(
                any(List.class),
                any(LocalDateTime.class),
                anyInt(),
                anyLong()))
                .thenReturn(Flux.just(order1, order2));

        StepVerifier.create(orderService.findOrdersToProcess(10))
                .expectNext(order1)
                .expectNext(order2)
                .verifyComplete();
    }

    @Test
    void findOrdersToProcessByTypes_ShouldReturnOrdersOfSpecifiedTypes() {
        Order order1 = new Order();
        order1.setId(UUID.randomUUID());
        order1.setStatus("PENDING");
        order1.setType("STANDARD");

        when(orderRepository.findOrdersToProcessByTypes(
                any(List.class),
                any(LocalDateTime.class),
                any(List.class),
                anyInt()))
                .thenReturn(Flux.just(order1));

        StepVerifier.create(orderService.findOrdersToProcessByTypes(List.of("STANDARD", "PRIORITY"), 10))
                .expectNext(order1)
                .verifyComplete();
    }

    @Test
    void updateOrderStatus_ShouldUpdateAndReturnOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus("COMPLETED");

        when(orderRepository.updateOrderStatus(
                any(UUID.class),
                anyString(),
                anyString(),
                any(LocalDateTime.class)))
                .thenReturn(Mono.just(1));

        when(orderRepository.findById(any(UUID.class)))
                .thenReturn(Mono.just(order));

        StepVerifier.create(orderService.updateOrderStatus(orderId, "COMPLETED", "test-user"))
                .expectNext(order)
                .verifyComplete();
    }

    @Test
    void getOrderById_ShouldReturnOrder() {
        UUID orderId = UUID.randomUUID();
        Order order = new Order();
        order.setId(orderId);
        order.setStatus("PENDING");

        when(orderRepository.findById(any(UUID.class)))
                .thenReturn(Mono.just(order));

        StepVerifier.create(orderService.getOrderById(orderId))
                .expectNext(order)
                .verifyComplete();
    }

    @Test
    void createOrder_ShouldReturnCreatedOrder() {
        Order input = new Order();
        input.setStatus("PENDING");

        Order savedOrder = new Order();
        savedOrder.setId(UUID.randomUUID());
        savedOrder.setStatus("PENDING");

        when(orderRepository.save(any(Order.class)))
                .thenReturn(Mono.just(savedOrder));

        StepVerifier.create(orderService.createOrder(input))
                .expectNext(savedOrder)
                .verifyComplete();
    }

    @Test
    void deleteOrder_ShouldReturnTrue() {
        UUID orderId = UUID.randomUUID();
        String deletedBy = "test-user";

        when(orderRepository.softDeleteOrder(
                any(UUID.class),
                anyString(),
                any(LocalDateTime.class)))
                .thenReturn(Mono.just(1));

        StepVerifier.create(orderService.deleteOrder(orderId, deletedBy))
                .expectNext(true)
                .verifyComplete();
    }

    @Test
    void findOrdersByCustomer_ShouldReturnCustomerOrders() {
        UUID customerId = UUID.randomUUID();
        Order order1 = new Order();
        order1.setId(UUID.randomUUID());
        order1.setCustomerId(customerId);

        Order order2 = new Order();
        order2.setId(UUID.randomUUID());
        order2.setCustomerId(customerId);

        when(orderRepository.findByCustomerIdAndDeletedFalse(any(UUID.class)))
                .thenReturn(Flux.just(order1, order2));

        StepVerifier.create(orderService.findOrdersByCustomer(customerId))
                .expectNext(order1)
                .expectNext(order2)
                .verifyComplete();
    }
}