package dev.demo.order.async.processor.repository;

import dev.demo.order.async.processor.repository.model.Order;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderRepository extends R2dbcRepository<Order, UUID> {

    /**
     * Find orders to process based on status and creation date
     *
     * @param statuses List of status values to match
     * @param beforeDate Only consider orders created before this date
     * @param limit Maximum number of results
     * @param offset Pagination offset
     * @return Flux of orders
     */
    @Query("SELECT * FROM orders WHERE status IN (:statuses) AND created_at < :beforeDate AND deleted = false ORDER BY priority DESC, created_at LIMIT :limit OFFSET :offset")
    Flux<Order> findOrdersToProcess(List<String> statuses, LocalDateTime beforeDate, int limit, long offset);

    /**
     * Find orders to process with a more efficient query for large datasets
     *
     * @param statuses List of status values to match
     * @param beforeDate Only consider orders created before this date
     * @param types List of order types to match
     * @param limit Maximum number of results
     * @return Flux of orders
     */
    @Query("SELECT * FROM orders WHERE status IN (:statuses) AND created_at < :beforeDate AND type IN (:types) AND deleted = false ORDER BY priority DESC, created_at LIMIT :limit")
    Flux<Order> findOrdersToProcessByTypes(List<String> statuses, LocalDateTime beforeDate, List<String> types, int limit);

    /**
     * Count orders by status
     *
     * @param status Status to count
     * @return Count of orders
     */
    @Query("SELECT COUNT(*) FROM orders WHERE status = :status AND deleted = false")
    Mono<Long> countByStatus(String status);

    /**
     * Update order status in a non-blocking way
     *
     * @param id Order ID
     * @param status New status
     * @param updatedBy User who updated the order
     * @param updatedAt Updated timestamp
     * @return Number of rows affected
     */
    @Query("UPDATE orders SET status = :status, updated_by = :updatedBy, updated_at = :updatedAt WHERE id = :id AND deleted = false")
    Mono<Integer> updateOrderStatus(UUID id, String status, String updatedBy, LocalDateTime updatedAt);

    /**
     * Find orders due for processing
     *
     * @param dueDate Orders due before this date
     * @param statuses Statuses to include
     * @param limit Maximum number to return
     * @return Orders that need processing
     */
    @Query("SELECT * FROM orders WHERE due_date <= :dueDate AND status IN (:statuses) AND deleted = false ORDER BY priority DESC, due_date LIMIT :limit")
    Flux<Order> findOrdersDueForProcessing(LocalDateTime dueDate, List<String> statuses, int limit);

    /**
     * Find orders by customer ID
     *
     * @param customerId Customer ID
     * @return Orders for the customer
     */
    Flux<Order> findByCustomerIdAndDeletedFalse(UUID customerId);

    /**
     * Soft delete an order
     *
     * @param id Order ID
     * @param updatedBy User who deleted the order
     * @param updatedAt Timestamp of deletion
     * @return Number of rows affected
     */
    @Query("UPDATE orders SET deleted = true, updated_by = :updatedBy, updated_at = :updatedAt WHERE id = :id")
    Mono<Integer> softDeleteOrder(UUID id, String updatedBy, LocalDateTime updatedAt);
}