package dev.demo.order.async.processor.repository;

import dev.demo.order.async.processor.repository.model.OrderDocument;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Repository
public interface OrderDocumentRepository extends R2dbcRepository<OrderDocument, UUID> {

    /**
     * Find documents for a specific order
     *
     * @param orderId Order ID
     * @return Documents for the order
     */
    Flux<OrderDocument> findByOrderIdOrderByUploadedAtDesc(UUID orderId);

    /**
     * Find documents of a specific type for an order
     *
     * @param orderId Order ID
     * @param type Document type
     * @return Documents matching the criteria
     */
    Flux<OrderDocument> findByOrderIdAndTypeOrderByUploadedAtDesc(UUID orderId, String type);

    /**
     * Find documents by status
     *
     * @param status Document status
     * @return Documents with the specified status
     */
    Flux<OrderDocument> findByStatus(String status);

    /**
     * Find documents with expiry dates in a specific range
     *
     * @param startDate Range start
     * @param endDate Range end
     * @return Documents expiring in the range
     */
    Flux<OrderDocument> findByExpiryDateBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Find documents needing processing
     *
     * @param types Document types to consider
     * @param statuses Document statuses to consider
     * @param limit Maximum number to return
     * @return Documents needing processing
     */
    @Query("SELECT * FROM order_documents WHERE type IN (:types) AND status IN (:statuses) ORDER BY uploaded_at LIMIT :limit")
    Flux<OrderDocument> findDocumentsForProcessing(List<String> types, List<String> statuses, int limit);

    /**
     * Update document status
     *
     * @param id Document ID
     * @param status New status
     * @return Number of rows affected
     */
    @Query("UPDATE order_documents SET status = :status WHERE id = :id")
    Mono<Integer> updateDocumentStatus(UUID id, String status);
}