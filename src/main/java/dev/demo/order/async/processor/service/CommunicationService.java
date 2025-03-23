package dev.demo.order.async.processor.service;

import dev.demo.order.async.processor.repository.model.OrderCommunication;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface CommunicationService {

    /**
     * Get communication by ID
     *
     * @param id Communication ID
     * @return Communication if found
     */
    Mono<OrderCommunication> getCommunicationById(UUID id);

    /**
     * Find communications for an order
     *
     * @param orderId Order ID
     * @return Communications for the order
     */
    Flux<OrderCommunication> findCommunicationsByOrder(UUID orderId);

    /**
     * Find communications for a customer
     *
     * @param customerId Customer ID
     * @return Communications for the customer
     */
    Flux<OrderCommunication> findCommunicationsByCustomer(UUID customerId);

    /**
     * Create a new communication
     *
     * @param communication Communication to create
     * @return Created communication
     */
    Mono<OrderCommunication> createCommunication(OrderCommunication communication);

    /**
     * Update communication status
     *
     * @param id Communication ID
     * @param status New status
     * @param sentAt Time when sent
     * @return Success indicator
     */
    Mono<Boolean> updateCommunicationStatus(UUID id, String status, LocalDateTime sentAt);

    /**
     * Find communications that need to be sent
     *
     * @param statuses Statuses indicating not sent
     * @param batchSize Maximum number to return
     * @return Communications to send
     */
    Flux<OrderCommunication> findUnsentCommunications(List<String> statuses, int batchSize);

    /**
     * Process a communication (send it)
     *
     * @param communication Communication to process
     * @return Processed communication
     */
    Mono<OrderCommunication> processCommunication(OrderCommunication communication);

    /**
     * Mark communication as delivered
     *
     * @param id Communication ID
     * @param deliveredAt Time of delivery
     * @return Success indicator
     */
    Mono<Boolean> markCommunicationDelivered(UUID id, LocalDateTime deliveredAt);
}