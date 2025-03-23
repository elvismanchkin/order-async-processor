package dev.demo.order.async.processor;

import dev.demo.order.async.processor.repository.OrderDocumentRepository;
import dev.demo.order.async.processor.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.Scheduled;

import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Configuration for custom metrics registration and collection
 */
@Configuration
@RequiredArgsConstructor
@Slf4j
public class MetricsConfiguration {

    private final MeterRegistry meterRegistry;
    private final OrderRepository orderRepository;
    private final OrderDocumentRepository documentRepository;

    @Value("${metrics.collection.enabled:true}")
    private boolean metricsEnabled;

    // Metric objects
    private Counter orderProcessedCounter;
    private Counter orderErrorCounter;
    private Timer orderProcessingTimer;

    @PostConstruct
    public void init() {
        // Register metrics
        orderProcessedCounter = Counter.builder("order.processed")
                .description("Number of orders processed")
                .register(meterRegistry);

        orderErrorCounter = Counter.builder("order.errors")
                .description("Number of order processing errors")
                .register(meterRegistry);

        orderProcessingTimer = Timer.builder("order.processing.time")
                .description("Time taken to process orders")
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry);

        // Register custom gauges for order counts
        meterRegistry.gauge("order.count",
                Tags.of("status", "PENDING"),
                new AtomicInteger(0),
                AtomicInteger::get);

        meterRegistry.gauge("order.count",
                Tags.of("status", "PROCESSING"),
                new AtomicInteger(0),
                AtomicInteger::get);

        meterRegistry.gauge("order.count",
                Tags.of("status", "COMPLETED"),
                new AtomicInteger(0),
                AtomicInteger::get);

        meterRegistry.gauge("order.count",
                Tags.of("status", "ERROR"),
                new AtomicInteger(0),
                AtomicInteger::get);

        log.info("Custom metrics registered");
    }

    /**
     * Record a successful order processing
     */
    public void recordOrderProcessed() {
        orderProcessedCounter.increment();
    }

    /**
     * Record an order processing error
     */
    public void recordOrderError() {
        orderErrorCounter.increment();
    }

    /**
     * Record order processing time
     *
     * @param timeMs Time in milliseconds
     */
    public void recordOrderProcessingTime(long timeMs) {
        orderProcessingTimer.record(timeMs, TimeUnit.MILLISECONDS);
    }

    /**
     * Scheduled task to update gauge metrics with current counts
     */
    @Scheduled(fixedDelayString = "${metrics.collection.interval:60000}")
    public void updateMetrics() {
        if (!metricsEnabled) {
            return;
        }

        log.debug("Updating metrics");

        // Update order count gauges
        updateOrderCountGauge("PENDING");
        updateOrderCountGauge("PROCESSING");
        updateOrderCountGauge("COMPLETED");
        updateOrderCountGauge("ERROR");
    }

    /**
     * Update a specific order count gauge
     *
     * @param status Status to count
     */
    private void updateOrderCountGauge(String status) {
        orderRepository.countByStatus(status)
                .doOnNext(count -> {
                    AtomicInteger gauge = meterRegistry.find("order.count")
                            .tags("status", status)
                            .gauge();

                    if (gauge != null) {
                        gauge.set(count.intValue());
                    }
                })
                .subscribe();
    }
}