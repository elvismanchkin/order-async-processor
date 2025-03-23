package dev.demo.order.async.processor.service;

import dev.demo.order.async.processor.repository.model.Order;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.UUID;

public interface OrderService {
    /**
     * Find orders that are ready for processing
     *
     * @param batchSize Maximum number of orders to process in one batch
     * @return Flux of orders
     */
    Flux<Order> findOrdersToProcess(int batchSize);

    /**
     * Find orders of specific types that are ready for processing
     *
     * @param types List of order types to process
     * @param batchSize Maximum number of orders to process
     * @return Flux of orders
     */
    Flux<Order> findOrdersToProcessByTypes(List<String> types, int batchSize);

    /**
     * Find orders due for processing based on due date
     *
     * @param batchSize Maximum number of orders to process
     * @return Flux of orders
     */
    Flux<Order> findOrdersDueForProcessing(int batchSize);

    /**
     * Process a single order
     *
     * @param order The order to process
     * @return Mono with processed order or error
     */
    Mono<Order> processOrder(Order order);

    /**
     * Update an order's status
     *
     * @param id Order ID
     * @param status New status
     * @param updatedBy User making the update
     * @return Mono with updated order or error
     */
    Mono<Order> updateOrderStatus(UUID id, String status, String updatedBy);

    /**
     * Get an order by ID
     *
     * @param id Order ID
     * @return Order if found
     */
    Mono<Order> getOrderById(UUID id);

    /**
     * Create a new order
     *
     * @param order Order to create
     * @return Created order
     */
    Mono<Order> createOrder(Order order);

    /**
     * Soft delete an order
     *
     * @param id Order ID
     * @param deletedBy User performing the deletion
     * @return Success indicator
     */
    Mono<Boolean> deleteOrder(UUID id, String deletedBy);

    /**
     * Find orders for a customer
     *
     * @param customerId Customer ID
     * @return Orders for the customer
     */
    Flux<Order> findOrdersByCustomer(UUID customerId);
}