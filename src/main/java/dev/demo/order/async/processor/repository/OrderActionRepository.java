package dev.demo.order.async.processor.repository;

import dev.demo.order.async.processor.repository.model.OrderAction;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderActionRepository extends R2dbcRepository<OrderAction, UUID> {

    /**
     * Find actions for a specific order
     *
     * @param orderId Order ID
     * @return Actions for the order
     */
    Flux<OrderAction> findByOrderIdOrderByPerformedAtDesc(UUID orderId);

    /**
     * Find actions of a specific type for an order
     *
     * @param orderId Order ID
     * @param type Action type
     * @return Actions matching the criteria
     */
    Flux<OrderAction> findByOrderIdAndTypeOrderByPerformedAtDesc(UUID orderId, String type);

    /**
     * Find most recent action for an order
     *
     * @param orderId Order ID
     * @return The most recent action
     */
    @Query("SELECT * FROM order_actions WHERE order_id = :orderId ORDER BY performed_at DESC LIMIT 1")
    Mono<OrderAction> findMostRecentActionForOrder(UUID orderId);

    /**
     * Find failed actions that need retry
     *
     * @param types Action types to consider
     * @param statuses Statuses indicating failure
     * @param before Time threshold
     * @param limit Maximum number to return
     * @return Failed actions for retry
     */
    @Query("SELECT * FROM order_actions WHERE type IN (:types) AND status IN (:statuses) AND performed_at < :before ORDER BY performed_at LIMIT :limit")
    Flux<OrderAction> findFailedActionsForRetry(List<String> types, List<String> statuses, LocalDateTime before, int limit);

    /**
     * Count actions by status for a specified timeframe
     *
     * @param status Action status
     * @param start Start time
     * @param end End time
     * @return Count of actions
     */
    @Query("SELECT COUNT(*) FROM order_actions WHERE status = :status AND performed_at BETWEEN :start AND :end")
    Mono<Long> countActionsByStatusInTimeframe(String status, LocalDateTime start, LocalDateTime end);
}