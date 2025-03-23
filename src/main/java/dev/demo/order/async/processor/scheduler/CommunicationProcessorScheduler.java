package dev.demo.order.async.processor.scheduler;

import dev.demo.order.async.processor.repository.model.OrderCommunication;
import dev.demo.order.async.processor.service.CommunicationService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommunicationProcessorScheduler {

    private final CommunicationService communicationService;

    @Value("${communication.processing.batch-size:50}")
    private int batchSize;

    @Value("${communication.processing.concurrency:10}")
    private int concurrency;

    @Value("${communication.processing.statuses:PENDING}")
    private List<String> pendingStatuses;

    @Value("${communication.processing.enabled:true}")
    private boolean enabled;

    /**
     * Scheduled task to process pending communications
     */
    @Scheduled(fixedDelayString = "${communication.processing.interval:30000}")
    @Observed(name = "communication.scheduler.process", contextualName = "processCommunicationsScheduled")
    public void processCommunications() {
        if (!enabled) {
            log.info("Communication processing is disabled");
            return;
        }

        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger errorCounter = new AtomicInteger(0);

        log.info("Starting communication processing batch. Batch size: {}", batchSize);

        communicationService.findUnsentCommunications(pendingStatuses, batchSize)
                .doOnNext(comm -> log.debug("Processing communication: {}", comm.getId()))
                .flatMap(this::processCommunication, concurrency)
                .doOnNext(success -> {
                    if (success) {
                        counter.incrementAndGet();
                    } else {
                        errorCounter.incrementAndGet();
                    }
                })
                .doOnComplete(() -> log.info("Completed communication processing batch. Processed: {}, Errors: {}",
                        counter.get(), errorCounter.get()))
                .onErrorContinue((error, obj) -> {
                    log.error("Error during communication processing batch: {}", error.getMessage(), error);
                    errorCounter.incrementAndGet();
                })
                .subscribe();
    }

    /**
     * Scheduled task to simulate checking for delivery status updates
     * (in a real system, this might be a webhook or message queue consumer)
     */
    @Scheduled(fixedDelayString = "${communication.delivery.check.interval:300000}")
    @Observed(name = "communication.scheduler.check.delivery", contextualName = "checkDeliveryStatusScheduled")
    public void checkDeliveryStatus() {
        if (!enabled) {
            log.info("Communication processing is disabled");
            return;
        }

        log.info("Checking for communication delivery status updates");

        // In a real implementation, this would integrate with an external service
        // to check for delivery status updates. For simulation purposes, we'll
        // just generate a log message.
        log.info("Delivery status check completed");
    }

    /**
     * Process a single communication
     *
     * @param communication Communication to process
     * @return Success indicator
     */
    @Observed(name = "communication.scheduler.process.single", contextualName = "processCommunication")
    private Mono<Boolean> processCommunication(OrderCommunication communication) {
        return communicationService.processCommunication(communication)
                .flatMap(processedComm -> {
                    // Simulate a random delivery confirmation for some messages
                    if (Math.random() > 0.7) {
                        log.debug("Simulating delivery confirmation for communication: {}", processedComm.getId());
                        return communicationService.markCommunicationDelivered(
                                processedComm.getId(),
                                LocalDateTime.now().plusSeconds((long)(Math.random() * 60))
                        );
                    }
                    return Mono.just(true);
                })
                .onErrorResume(error -> {
                    log.error("Error processing communication {}: {}", communication.getId(), error.getMessage(), error);
                    return Mono.just(false);
                });
    }
}