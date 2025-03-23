package dev.demo.order.async.processor.service;

import dev.demo.order.async.processor.repository.OrderDocumentRepository;
import dev.demo.order.async.processor.repository.model.OrderDocument;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DocumentServiceImpl implements DocumentService {

    private final OrderDocumentRepository documentRepository;

    @Value("${document.processing.status.pending:PENDING}")
    private String pendingStatus;

    @Value("${document.processing.status.processing:PROCESSING}")
    private String processingStatus;

    @Value("${document.processing.status.completed:COMPLETED}")
    private String completedStatus;

    @Value("${document.processing.status.error:ERROR}")
    private String errorStatus;

    @Override
    @Observed(name = "document.service.get", contextualName = "getDocumentById")
    public Mono<OrderDocument> getDocumentById(UUID id) {
        return documentRepository.findById(id)
                .doOnNext(document -> log.debug("Retrieved document: {}", document.getId()))
                .doOnError(error -> log.error("Error retrieving document {}: {}", id, error.getMessage(), error));
    }

    @Override
    @Observed(name = "document.service.find.by.order", contextualName = "findDocumentsByOrder")
    public Flux<OrderDocument> findDocumentsByOrder(UUID orderId) {
        return documentRepository.findByOrderIdOrderByUploadedAtDesc(orderId)
                .doOnComplete(() -> log.debug("Found documents for order: {}", orderId));
    }

    @Override
    @Observed(name = "document.service.find.by.order.type", contextualName = "findDocumentsByOrderAndType")
    public Flux<OrderDocument> findDocumentsByOrderAndType(UUID orderId, String type) {
        return documentRepository.findByOrderIdAndTypeOrderByUploadedAtDesc(orderId, type)
                .doOnComplete(() -> log.debug("Found documents for order {} and type {}", orderId, type));
    }

    @Override
    @Transactional
    @Observed(name = "document.service.create", contextualName = "createDocument")
    public Mono<OrderDocument> createDocument(OrderDocument document) {
        if (document.getId() == null) {
            document.setId(UUID.randomUUID());
        }

        if (document.getUploadedAt() == null) {
            document.setUploadedAt(LocalDateTime.now());
        }

        if (document.getStatus() == null) {
            document.setStatus(pendingStatus);
        }

        return documentRepository.save(document)
                .doOnNext(savedDocument -> log.info("Created new document: {} for order: {}",
                        savedDocument.getId(), savedDocument.getOrderId()));
    }

    @Override
    @Transactional
    @Observed(name = "document.service.update.status", contextualName = "updateDocumentStatus")
    public Mono<Boolean> updateDocumentStatus(UUID id, String status) {
        return documentRepository.updateDocumentStatus(id, status)
                .map(result -> result > 0)
                .doOnNext(success -> {
                    if (success) {
                        log.info("Updated document {} status to {}", id, status);
                    } else {
                        log.warn("Failed to update document {} status", id);
                    }
                });
    }

    @Override
    @Observed(name = "document.service.find.for.processing", contextualName = "findDocumentsForProcessing")
    public Flux<OrderDocument> findDocumentsForProcessing(List<String> types, List<String> statuses, int batchSize) {
        return documentRepository.findDocumentsForProcessing(types, statuses, batchSize)
                .doOnNext(document -> log.debug("Found document for processing: {}", document.getId()))
                .doOnComplete(() -> log.debug("Found documents for processing, types: {}, statuses: {}", types, statuses));
    }

    @Override
    @Observed(name = "document.service.find.expiring", contextualName = "findDocumentsExpiringBetween")
    public Flux<OrderDocument> findDocumentsExpiringBetween(LocalDate startDate, LocalDate endDate) {
        return documentRepository.findByExpiryDateBetween(startDate, endDate)
                .doOnComplete(() -> log.debug("Found documents expiring between {} and {}", startDate, endDate));
    }

    @Override
    @Transactional
    @Observed(name = "document.service.process", contextualName = "processDocument")
    public Mono<OrderDocument> processDocument(OrderDocument document) {
        log.info("Processing document: {}", document.getId());

        // First update status to processing
        return updateDocumentStatus(document.getId(), processingStatus)
                .flatMap(success -> {
                    if (!success) {
                        return Mono.error(new RuntimeException("Failed to update document status to processing"));
                    }

                    // Retrieve the latest document
                    return documentRepository.findById(document.getId());
                })
                .flatMap(processedDocument -> {
                    // Simulated document processing logic
                    log.info("Performing document processing for: {}", processedDocument.getId());

                    // Update status to completed after processing
                    return updateDocumentStatus(processedDocument.getId(), completedStatus)
                            .flatMap(success -> {
                                if (!success) {
                                    return Mono.error(new RuntimeException("Failed to update document status to completed"));
                                }
                                return documentRepository.findById(processedDocument.getId());
                            });
                })
                .onErrorResume(error -> {
                    log.error("Error processing document {}: {}", document.getId(), error.getMessage(), error);
                    return updateDocumentStatus(document.getId(), errorStatus)
                            .then(documentRepository.findById(document.getId()));
                });
    }
}