package dev.demo.order.async.processor.service;

import dev.demo.order.async.processor.repository.model.OrderDocument;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface DocumentService {

    /**
     * Get document by ID
     *
     * @param id Document ID
     * @return Document if found
     */
    Mono<OrderDocument> getDocumentById(UUID id);

    /**
     * Find documents for an order
     *
     * @param orderId Order ID
     * @return Documents for the order
     */
    Flux<OrderDocument> findDocumentsByOrder(UUID orderId);

    /**
     * Find documents by type for an order
     *
     * @param orderId Order ID
     * @param type Document type
     * @return Documents matching criteria
     */
    Flux<OrderDocument> findDocumentsByOrderAndType(UUID orderId, String type);

    /**
     * Create a new document
     *
     * @param document Document to create
     * @return Created document
     */
    Mono<OrderDocument> createDocument(OrderDocument document);

    /**
     * Update document status
     *
     * @param id Document ID
     * @param status New status
     * @return Success indicator
     */
    Mono<Boolean> updateDocumentStatus(UUID id, String status);

    /**
     * Find documents that need processing
     *
     * @param types Document types to consider
     * @param statuses Statuses to consider
     * @param batchSize Maximum number to return
     * @return Documents for processing
     */
    Flux<OrderDocument> findDocumentsForProcessing(List<String> types, List<String> statuses, int batchSize);

    /**
     * Find documents with expiry dates in a date range
     *
     * @param startDate Range start
     * @param endDate Range end
     * @return Documents expiring in range
     */
    Flux<OrderDocument> findDocumentsExpiringBetween(LocalDate startDate, LocalDate endDate);

    /**
     * Process a document
     *
     * @param document Document to process
     * @return Processed document
     */
    Mono<OrderDocument> processDocument(OrderDocument document);
}