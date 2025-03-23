package dev.demo.order.async.processor.repository;

import dev.demo.order.async.processor.repository.model.OrderCommunication;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderCommunicationRepository extends R2dbcRepository<OrderCommunication, UUID> {

    /**
     * Find communications for a specific order
     *
     * @param orderId Order ID
     * @return Communications for the order
     */
    Flux<OrderCommunication> findByOrderIdOrderByCreatedAtDesc(UUID orderId);

    /**
     * Find communications for a specific customer
     *
     * @param customerId Customer ID
     * @return Communications for the customer
     */
    Flux<OrderCommunication> findByCustomerIdOrderByCreatedAtDesc(UUID customerId);

    /**
     * Find communications by type and channel
     *
     * @param type Communication type
     * @param channel Communication channel
     * @return Communications matching the criteria
     */
    Flux<OrderCommunication> findByTypeAndChannel(String type, String channel);

    /**
     * Find unsent communications
     *
     * @param statuses Statuses indicating not sent
     * @param limit Maximum number to return
     * @return Unsent communications
     */
    @Query("SELECT * FROM order_communications WHERE status IN (:statuses) ORDER BY created_at LIMIT :limit")
    Flux<OrderCommunication> findUnsentCommunications(List<String> statuses, int limit);

    /**
     * Update communication status
     *
     * @param id Communication ID
     * @param status New status
     * @param sentAt Time when sent
     * @return Number of rows affected
     */
    @Query("UPDATE order_communications SET status = :status, sent_at = :sentAt WHERE id = :id")
    Mono<Integer> updateCommunicationStatus(UUID id, String status, LocalDateTime sentAt);

    /**
     * Count communications by channel in a timeframe
     *
     * @param channel Communication channel
     * @param start Start time
     * @param end End time
     * @return Count of communications
     */
    @Query("SELECT COUNT(*) FROM order_communications WHERE channel = :channel AND created_at BETWEEN :start AND :end")
    Mono<Long> countByChannelInTimeframe(String channel, LocalDateTime start, LocalDateTime end);
}