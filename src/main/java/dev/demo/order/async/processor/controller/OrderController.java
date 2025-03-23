package dev.demo.order.async.processor.controller;

import dev.demo.order.async.processor.repository.model.Order;
import dev.demo.order.async.processor.service.OrderService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * API endpoints for order management
 */
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
@Slf4j
public class OrderController {

    private final OrderService orderService;

    /**
     * Get an order by ID
     */
    @GetMapping("/{id}")
    @Observed(name = "api.order.get", contextualName = "apiGetOrderById")
    public Mono<ResponseEntity<Order>> getOrderById(@PathVariable UUID id) {
        return orderService.getOrderById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(error -> log.error("Error retrieving order {}: {}", id, error.getMessage(), error));
    }

    /**
     * Create a new order
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Observed(name = "api.order.create", contextualName = "apiCreateOrder")
    public Mono<Order> createOrder(@RequestBody Order order) {
        return orderService.createOrder(order)
                .doOnSuccess(createdOrder -> log.info("Created order: {}", createdOrder.getId()))
                .doOnError(error -> log.error("Error creating order: {}", error.getMessage(), error));
    }

    /**
     * Update an order status
     */
    @PatchMapping("/{id}/status")
    @Observed(name = "api.order.update.status", contextualName = "apiUpdateOrderStatus")
    public Mono<Order> updateOrderStatus(
            @PathVariable UUID id,
            @RequestParam String status,
            @RequestParam String updatedBy) {

        return orderService.updateOrderStatus(id, status, updatedBy)
                .doOnSuccess(updatedOrder -> log.info("Updated order {} status to {}", id, status))
                .doOnError(error -> log.error("Error updating order {} status: {}", id, error.getMessage(), error));
    }

    /**
     * Delete an order
     */
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Observed(name = "api.order.delete", contextualName = "apiDeleteOrder")
    public Mono<Void> deleteOrder(@PathVariable UUID id, @RequestParam String deletedBy) {
        return orderService.deleteOrder(id, deletedBy)
                .flatMap(success -> {
                    if (success) {
                        return Mono.empty();
                    } else {
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
                    }
                })
                .doOnSuccess(v -> log.info("Deleted order: {}", id))
                .doOnError(error -> log.error("Error deleting order {}: {}", id, error.getMessage(), error))
                .then();
    }

    /**
     * Get orders for a customer
     */
    @GetMapping("/customer/{customerId}")
    @Observed(name = "api.order.find.by.customer", contextualName = "apiFindOrdersByCustomer")
    public Flux<Order> getOrdersByCustomer(@PathVariable UUID customerId) {
        return orderService.findOrdersByCustomer(customerId)
                .doOnComplete(() -> log.info("Retrieved orders for customer: {}", customerId))
                .doOnError(error -> log.error("Error retrieving orders for customer {}: {}",
                        customerId, error.getMessage(), error));
    }

    /**
     * Process an order manually
     */
    @PostMapping("/{id}/process")
    @Observed(name = "api.order.process", contextualName = "apiProcessOrder")
    public Mono<Order> processOrder(@PathVariable UUID id) {
        return orderService.getOrderById(id)
                .flatMap(orderService::processOrder)
                .doOnSuccess(processedOrder -> log.info("Manually processed order: {}", id))
                .doOnError(error -> log.error("Error processing order {}: {}", id, error.getMessage(), error));
    }
}