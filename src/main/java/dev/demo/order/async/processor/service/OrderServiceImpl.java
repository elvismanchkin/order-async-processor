package dev.demo.order.async.processor.service;

import dev.demo.order.async.processor.repository.OrderActionRepository;
import dev.demo.order.async.processor.repository.OrderRepository;
import dev.demo.order.async.processor.repository.model.Order;
import dev.demo.order.async.processor.repository.model.OrderAction;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderServiceImpl implements OrderService {

    private final OrderRepository orderRepository;
    private final OrderActionRepository actionRepository;

    @Value("${order.processing.status.pending:PENDING}")
    private String pendingStatus;

    @Value("${order.processing.status.processing:PROCESSING}")
    private String processingStatus;

    @Value("${order.processing.status.completed:COMPLETED}")
    private String completedStatus;

    @Value("${order.processing.status.error:ERROR}")
    private String errorStatus;

    @Value("${order.processing.max-age:24h}")
    private Duration maxAge;

    @Override
    @Observed(name = "order.service.find", contextualName = "findOrdersToProcess")
    public Flux<Order> findOrdersToProcess(int batchSize) {
        LocalDateTime cutoffTime = LocalDateTime.now().minus(maxAge);
        log.debug("Finding orders to process, batch size: {}, cutoff time: {}", batchSize, cutoffTime);

        return orderRepository.findOrdersToProcess(
                        List.of(pendingStatus),
                        cutoffTime,
                        batchSize,
                        0
                ).doOnNext(order -> log.debug("Found order to process: {}", order.getId()))
                .doOnComplete(() -> log.debug("Completed finding orders to process"));
    }

    @Override
    @Observed(name = "order.service.find.by.types", contextualName = "findOrdersToProcessByTypes")
    public Flux<Order> findOrdersToProcessByTypes(List<String> types, int batchSize) {
        LocalDateTime cutoffTime = LocalDateTime.now().minus(maxAge);
        log.debug("Finding orders to process by types: {}, batch size: {}", types, batchSize);

        return orderRepository.findOrdersToProcessByTypes(
                        List.of(pendingStatus),
                        cutoffTime,
                        types,
                        batchSize
                ).doOnNext(order -> log.debug("Found order to process: {}", order.getId()))
                .doOnComplete(() -> log.debug("Completed finding orders to process by types"));
    }

    @Override
    @Observed(name = "order.service.find.due", contextualName = "findOrdersDueForProcessing")
    public Flux<Order> findOrdersDueForProcessing(int batchSize) {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Finding orders due for processing, batch size: {}, current time: {}", batchSize, now);

        return orderRepository.findOrdersDueForProcessing(
                        now,
                        List.of(pendingStatus),
                        batchSize
                ).doOnNext(order -> log.debug("Found order due for processing: {}", order.getId()))
                .doOnComplete(() -> log.debug("Completed finding orders due for processing"));
    }

    @Override
    @Transactional
    @Observed(name = "order.service.process", contextualName = "processOrder")
    public Mono<Order> processOrder(Order order) {
        log.info("Processing order: {}", order.getId());

        // Create action record for processing start
        OrderAction action = new OrderAction();
        action.setId(UUID.randomUUID());
        action.setOrderId(order.getId());
        action.setType("PROCESS");
        action.setStatus("STARTED");
        action.setPerformedBy("system");
        action.setPerformedAt(LocalDateTime.now());
        action.setDescription("Starting order processing");

        // First update status to processing and create action record
        return actionRepository.save(action)
                .then(updateOrderStatus(order.getId(), processingStatus, "system"))
                .flatMap(processedOrder -> {
                    // Simulated processing logic
                    log.info("Performing business logic for order: {}", processedOrder.getId());

                    // Create action record for completion
                    OrderAction completeAction = new OrderAction();
                    completeAction.setId(UUID.randomUUID());
                    completeAction.setOrderId(order.getId());
                    completeAction.setType("PROCESS");
                    completeAction.setStatus("COMPLETED");
                    completeAction.setPerformedBy("system");
                    completeAction.setPerformedAt(LocalDateTime.now());
                    completeAction.setDescription("Order processing completed");
                    completeAction.setResult("SUCCESS");

                    // Update status to completed after processing
                    return actionRepository.save(completeAction)
                            .then(updateOrderStatus(processedOrder.getId(), completedStatus, "system"));
                })
                .onErrorResume(error -> {
                    log.error("Error processing order {}: {}", order.getId(), error.getMessage(), error);

                    // Create action record for error
                    OrderAction errorAction = new OrderAction();
                    errorAction.setId(UUID.randomUUID());
                    errorAction.setOrderId(order.getId());
                    errorAction.setType("PROCESS");
                    errorAction.setStatus("FAILED");
                    errorAction.setPerformedBy("system");
                    errorAction.setPerformedAt(LocalDateTime.now());
                    errorAction.setDescription("Order processing failed");
                    errorAction.setErrorCode("ERR-001");
                    errorAction.setErrorMessage(error.getMessage());

                    return actionRepository.save(errorAction)
                            .then(updateOrderStatus(order.getId(), errorStatus, "system"));
                });
    }

    @Override
    @Transactional
    @Observed(name = "order.service.update.status", contextualName = "updateOrderStatus")
    public Mono<Order> updateOrderStatus(UUID id, String status, String updatedBy) {
        LocalDateTime now = LocalDateTime.now();
        log.debug("Updating order {} status to {}", id, status);

        return orderRepository.updateOrderStatus(id, status, updatedBy, now)
                .then(orderRepository.findById(id))
                .doOnNext(order -> log.debug("Updated order {} status to {}", id, status));
    }

    @Override
    @Observed(name = "order.service.get", contextualName = "getOrderById")
    public Mono<Order> getOrderById(UUID id) {
        return orderRepository.findById(id)
                .doOnNext(order -> log.debug("Retrieved order: {}", order.getId()))
                .doOnError(error -> log.error("Error retrieving order {}: {}", id, error.getMessage()));
    }

    @Override
    @Transactional
    @Observed(name = "order.service.create", contextualName = "createOrder")
    public Mono<Order> createOrder(Order order) {
        if (order.getId() == null) {
            order.setId(UUID.randomUUID());
        }

        if (order.getCreatedAt() == null) {
            order.setCreatedAt(LocalDateTime.now());
        }

        return orderRepository.save(order)
                .doOnNext(savedOrder -> {
                    log.info("Created new order: {}", savedOrder.getId());

                    // Create action record for order creation
                    OrderAction action = new OrderAction();
                    action.setId(UUID.randomUUID());
                    action.setOrderId(savedOrder.getId());
                    action.setType("CREATE");
                    action.setStatus("COMPLETED");
                    action.setPerformedBy(savedOrder.getCreatedBy());
                    action.setPerformedAt(savedOrder.getCreatedAt());
                    action.setDescription("Order created");

                    actionRepository.save(action).subscribe();
                });
    }

    @Override
    @Transactional
    @Observed(name = "order.service.delete", contextualName = "deleteOrder")
    public Mono<Boolean> deleteOrder(UUID id, String deletedBy) {
        log.info("Deleting order: {}", id);

        return orderRepository.softDeleteOrder(id, deletedBy, LocalDateTime.now())
                .map(result -> result > 0)
                .doOnNext(success -> {
                    if (success) {
                        log.info("Order deleted: {}", id);

                        // Create action record for deletion
                        OrderAction action = new OrderAction();
                        action.setId(UUID.randomUUID());
                        action.setOrderId(id);
                        action.setType("DELETE");
                        action.setStatus("COMPLETED");
                        action.setPerformedBy(deletedBy);
                        action.setPerformedAt(LocalDateTime.now());
                        action.setDescription("Order deleted");

                        actionRepository.save(action).subscribe();
                    } else {
                        log.warn("Failed to delete order: {}", id);
                    }
                });
    }

    @Override
    @Observed(name = "order.service.find.by.customer", contextualName = "findOrdersByCustomer")
    public Flux<Order> findOrdersByCustomer(UUID customerId) {
        return orderRepository.findByCustomerIdAndDeletedFalse(customerId)
                .doOnComplete(() -> log.debug("Found orders for customer: {}", customerId));
    }
}