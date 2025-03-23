package dev.demo.order.async.processor.controller;

import dev.demo.order.async.processor.repository.model.OrderDocument;
import dev.demo.order.async.processor.service.DocumentService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.UUID;

/**
 * API endpoints for document management
 */
@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    /**
     * Get a document by ID
     */
    @GetMapping("/{id}")
    @Observed(name = "api.document.get", contextualName = "apiGetDocumentById")
    public Mono<ResponseEntity<OrderDocument>> getDocumentById(@PathVariable UUID id) {
        return documentService.getDocumentById(id)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .doOnError(error -> log.error("Error retrieving document {}: {}", id, error.getMessage(), error));
    }

    /**
     * Get documents for an order
     */
    @GetMapping("/order/{orderId}")
    @Observed(name = "api.document.find.by.order", contextualName = "apiFindDocumentsByOrder")
    public Flux<OrderDocument> getDocumentsByOrder(@PathVariable UUID orderId) {
        return documentService.findDocumentsByOrder(orderId)
                .doOnComplete(() -> log.info("Retrieved documents for order: {}", orderId))
                .doOnError(error -> log.error("Error retrieving documents for order {}: {}",
                        orderId, error.getMessage(), error));
    }

    /**
     * Create a new document
     */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Observed(name = "api.document.create", contextualName = "apiCreateDocument")
    public Mono<OrderDocument> createDocument(@RequestBody OrderDocument document) {
        return documentService.createDocument(document)
                .doOnSuccess(createdDocument -> log.info("Created document: {} for order: {}",
                        createdDocument.getId(), createdDocument.getOrderId()))
                .doOnError(error -> log.error("Error creating document: {}", error.getMessage(), error));
    }

    /**
     * Update document status
     */
    @PatchMapping("/{id}/status")
    @Observed(name = "api.document.update.status", contextualName = "apiUpdateDocumentStatus")
    public Mono<ResponseEntity<Object>> updateDocumentStatus(
            @PathVariable UUID id,
            @RequestParam String status) {

        return documentService.updateDocumentStatus(id, status)
                .map(success -> {
                    if (success) {
                        return ResponseEntity.ok().build();
                    } else {
                        return ResponseEntity.notFound().build();
                    }
                })
                .doOnSuccess(result -> log.info("Updated document {} status to {}", id, status))
                .doOnError(error -> log.error("Error updating document {} status: {}",
                        id, error.getMessage(), error));
    }

    /**
     * Process a document manually
     */
    @PostMapping("/{id}/process")
    @Observed(name = "api.document.process", contextualName = "apiProcessDocument")
    public Mono<OrderDocument> processDocument(@PathVariable UUID id) {
        return documentService.getDocumentById(id)
                .flatMap(documentService::processDocument)
                .doOnSuccess(processedDocument -> log.info("Manually processed document: {}", id))
                .doOnError(error -> log.error("Error processing document {}: {}", id, error.getMessage(), error));
    }

    /**
     * Find documents expiring in a date range
     */
    @GetMapping("/expiring")
    @Observed(name = "api.document.find.expiring", contextualName = "apiFindExpiringDocuments")
    public Flux<OrderDocument> findExpiringDocuments(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        return documentService.findDocumentsExpiringBetween(startDate, endDate)
                .doOnComplete(() -> log.info("Found documents expiring between {} and {}", startDate, endDate))
                .doOnError(error -> log.error("Error finding expiring documents: {}", error.getMessage(), error));
    }
}