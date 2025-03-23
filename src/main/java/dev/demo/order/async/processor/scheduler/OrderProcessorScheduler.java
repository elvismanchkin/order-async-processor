package dev.demo.order.async.processor.scheduler;

import dev.demo.order.async.processor.client.ExternalServiceClient;
import dev.demo.order.async.processor.repository.model.Order;
import dev.demo.order.async.processor.service.OrderService;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProcessorScheduler {

    private final OrderService orderService;
    private final ExternalServiceClient externalServiceClient;

    @Value("${order.processing.batch-size:100}")
    private int batchSize;

    @Value("${order.processing.concurrency:10}")
    private int concurrency;

    @Value("${order.processing.types:STANDARD,PRIORITY}")
    private List<String> orderTypes;

    @Value("${order.processing.enabled:true}")
    private boolean enabled;

    @Value("${order.processing.backpressure-timeout:30s}")
    private Duration backpressureTimeout;

    /**
     * Scheduled task to process pending orders by type
     */
    @Scheduled(fixedDelayString = "${order.processing.interval:60000}")
    @Observed(name = "order.scheduler.process", contextualName = "processOrdersScheduled")
    public void processOrders() {
        if (!enabled) {
            log.info("Order processing is disabled");
            return;
        }

        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger errorCounter = new AtomicInteger(0);

        log.info("Starting order processing batch. Batch size: {}, Types: {}", batchSize, orderTypes);

        orderService
                .findOrdersToProcessByTypes(orderTypes, batchSize)
                .doOnNext(order -> log.debug("Processing order: {}", order.getId()))
                .flatMap(this::processOrderWithExternalServices, concurrency)
                .doOnNext(success -> {
                    if (success) {
                        counter.incrementAndGet();
                    } else {
                        errorCounter.incrementAndGet();
                    }
                })
                .doOnComplete(() -> log.info(
                        "Completed order processing batch. Processed: {}, Errors: {}",
                        counter.get(),
                        errorCounter.get()))
                .onErrorContinue((error, obj) -> {
                    log.error("Error during order processing batch: {}", error.getMessage(), error);
                    errorCounter.incrementAndGet();
                })
                .subscribe();
    }

    /**
     * Scheduled task to process orders due for processing based on due date
     */
    @Scheduled(fixedDelayString = "${order.processing.due-interval:300000}")
    @Observed(name = "order.scheduler.process.due", contextualName = "processDueOrdersScheduled")
    public void processDueOrders() {
        if (!enabled) {
            log.info("Order processing is disabled");
            return;
        }

        AtomicInteger counter = new AtomicInteger(0);
        AtomicInteger errorCounter = new AtomicInteger(0);

        log.info("Starting due order processing batch. Batch size: {}", batchSize);

        orderService
                .findOrdersDueForProcessing(batchSize)
                .doOnNext(order -> log.debug("Processing due order: {}", order.getId()))
                .flatMap(this::processOrderWithExternalServices, concurrency)
                .doOnNext(success -> {
                    if (success) {
                        counter.incrementAndGet();
                    } else {
                        errorCounter.incrementAndGet();
                    }
                })
                .doOnComplete(() -> log.info(
                        "Completed due order processing batch. Processed: {}, Errors: {}",
                        counter.get(),
                        errorCounter.get()))
                .onErrorContinue((error, obj) -> {
                    log.error("Error during due order processing batch: {}", error.getMessage(), error);
                    errorCounter.incrementAndGet();
                })
                .subscribe();
    }

    /**
     * Process a single order using external services
     *
     * @param order Order to process
     * @return Success indicator
     */
    @Observed(name = "order.scheduler.process.single", contextualName = "processOrderWithExternalServices")
    private Mono<Boolean> processOrderWithExternalServices(Order order) {
        return externalServiceClient
                .validateOrder(order)
                .flatMap(valid -> {
                    if (valid) {
                        log.debug("Order {} validated successfully, processing", order.getId());
                        return externalServiceClient.processOrder(order).flatMap(processedOrder -> {
                            log.debug("Order {} processed successfully, updating status", processedOrder.getId());
                            return orderService
                                    .updateOrderStatus(processedOrder.getId(), "COMPLETED", "system")
                                    .flatMap(updatedOrder -> {
                                        log.debug(
                                                "Order {} status updated, sending notification", updatedOrder.getId());
                                        return externalServiceClient
                                                .notifyOrderComplete(updatedOrder)
                                                .thenReturn(true);
                                    });
                        });
                    } else {
                        log.warn("Order {} failed validation", order.getId());
                        return orderService
                                .updateOrderStatus(order.getId(), "VALIDATION_FAILED", "system")
                                .thenReturn(false);
                    }
                })
                .timeout(backpressureTimeout)
                .onErrorResume(error -> {
                    log.error("Error processing order {}: {}", order.getId(), error.getMessage(), error);
                    return orderService
                            .updateOrderStatus(order.getId(), "ERROR", "system")
                            .thenReturn(false);
                });
    }
}