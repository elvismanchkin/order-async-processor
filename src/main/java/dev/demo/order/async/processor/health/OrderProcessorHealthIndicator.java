package dev.demo.order.async.processor.health;

import dev.demo.order.async.processor.repository.OrderActionRepository;
import dev.demo.order.async.processor.repository.OrderCommunicationRepository;
import dev.demo.order.async.processor.repository.OrderDocumentRepository;
import dev.demo.order.async.processor.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.Map;

/**
 * Custom health indicator that reports on the status of order processing
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderProcessorHealthIndicator implements ReactiveHealthIndicator {

    @Value("${health.order-processor.pending-threshold:1000}")
    private int pendingThreshold;

    @Value("${health.order-processor.error-threshold:100}")
    private int errorThreshold;

    private final OrderRepository orderRepository;
    private final OrderActionRepository actionRepository;
    private final OrderDocumentRepository documentRepository;
    private final OrderCommunicationRepository communicationRepository;

    @Override
    public Mono<Health> health() {
        return checkOrderCounts()
                .map(counts -> {
                    // If too many pending orders, report as DOWN
                    if (counts.get("orders.PENDING") != null && 
                        ((Number)counts.get("orders.PENDING")).longValue() > pendingThreshold) {
                        return Health.down()
                                .withDetail("reason", "Too many pending orders: " + 
                                           counts.get("orders.PENDING") + 
                                           " (threshold: " + pendingThreshold + ")")
                                .withDetails(counts)
                                .build();
                    }

                    return Health.up()
                            .withDetails(counts)
                            .build();
                })
                .onErrorResume(error -> {
                    log.error("Error checking order processor health: {}", error.getMessage(), error);
                    return Mono.just(Health.down()
                            .withException(error)
                            .build());
                });
    }

    private Mono<Map<String, Object>> checkOrderCounts() {
        Map<String, Object> counts = new HashMap<>();

        // Get counts for different order statuses
        return orderRepository.countByStatus("PENDING")
                .doOnNext(count -> counts.put("orders.PENDING", count))
                .then(orderRepository.countByStatus("PROCESSING"))
                .doOnNext(count -> counts.put("orders.PROCESSING", count))
                .then(orderRepository.countByStatus("COMPLETED"))
                .doOnNext(count -> counts.put("orders.COMPLETED", count))
                .then(orderRepository.countByStatus("ERROR"))
                .doOnNext(count -> counts.put("orders.ERROR", count))
                .thenReturn(counts);
    }
}