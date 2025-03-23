package dev.demo.order.async.processor.scheduler;

import dev.demo.order.async.processor.repository.model.OrderDocument;
import dev.demo.order.async.processor.service.DocumentService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class DocumentProcessorScheduler {

    private final DocumentService documentService;

    @Value("${document.processing.batch-size:50}")
    private int batchSize;

    @Value("${document.processing.concurrency:5}")
    private int concurrency;

    @Value("${document.processing.types:INVOICE,CONTRACT,RECEIPT}")
    private List<String> documentTypes;

    @Value("${document.processing.statuses:PENDING}")
    private List<String> pendingStatuses;

    @Value("${document.processing.enabled:true}")
    private boolean enabled;

    /**
     * Scheduled task to process pending documents
     */
    @Scheduled(fixedDelayString = "${document.processing.interval:120000}")
    @Observed(name = "document.scheduler.process", contextualName = "processDocumentsScheduled")
    public void processDocuments() {
        if (!enabled) {
            log.info("Document processing is disabled");
            return;
        }

        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger errorCounter = new AtomicInteger(0);

        log.info("Starting document processing batch. Batch size: {}, Types: {}", batchSize, documentTypes);

        documentService.findDocumentsForProcessing(documentTypes, pendingStatuses, batchSize)
                .doOnNext(document -> log.debug("Processing document: {}", document.getId()))
                .flatMap(this::processDocument, concurrency)
                .doOnNext(success -> {
                    if (success) {
                        counter.incrementAndGet();
                    } else {
                        errorCounter.incrementAndGet();
                    }
                })
                .doOnComplete(() -> log.info("Completed document processing batch. Processed: {}, Errors: {}",
                        counter.get(), errorCounter.get()))
                .onErrorContinue((error, obj) -> {
                    log.error("Error during document processing batch: {}", error.getMessage(), error);
                    errorCounter.incrementAndGet();
                })
                .subscribe();
    }

    /**
     * Scheduled task to check for expiring documents
     */
    @Scheduled(cron = "${document.expiry.cron:0 0 7 * * ?}") // Default: every day at 7:00 AM
    @Observed(name = "document.scheduler.check.expiry", contextualName = "checkExpiringDocumentsScheduled")
    public void checkExpiringDocuments() {
        if (!enabled) {
            log.info("Document processing is disabled");
            return;
        }

        // Check for documents expiring in the next 30 days
        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(30);

        log.info("Checking for documents expiring between {} and {}", startDate, endDate);

        AtomicInteger counter = new AtomicInteger(0);

        documentService.findDocumentsExpiringBetween(startDate, endDate)
                .doOnNext(document -> {
                    log.info("Document {} of type {} expires on {}",
                            document.getId(), document.getType(), document.getExpiryDate());
                    counter.incrementAndGet();
                })
                .doOnComplete(() -> log.info("Found {} documents expiring in the next 30 days", counter.get()))
                .subscribe();
    }

    /**
     * Process a single document
     *
     * @param document Document to process
     * @return Success indicator
     */
    @Observed(name = "document.scheduler.process.single", contextualName = "processDocument")
    private Mono<Boolean> processDocument(OrderDocument document) {
        return documentService.processDocument(document)
                .map(processedDocument -> true)
                .onErrorResume(error -> {
                    log.error("Error processing document {}: {}", document.getId(), error.getMessage(), error);
                    return Mono.just(false);
                });
    }
}