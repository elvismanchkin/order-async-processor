package dev.demo.order.async.processor.client;

import dev.demo.order.async.processor.repository.model.Order;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.observation.annotation.Observed;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class ExternalServiceClient {

    private final WebClient.Builder webClientBuilder;

    @Value("${service.external.url}")
    private String serviceUrl;

    @Value("${service.external.timeout:30s}")
    private Duration timeout;

    /**
     * Validate an order with the external validation service
     *
     * @param order Order to validate
     * @return True if valid, false otherwise
     */
    @CircuitBreaker(name = "externalServiceValidate")
    @Retry(name = "externalServiceValidate")
    @Observed(name = "external.service.validate", contextualName = "validateOrder")
    public Mono<Boolean> validateOrder(Order order) {
        log.debug("Validating order: {}", order.getId());

        return webClientBuilder
                .build()
                .post()
                .uri(serviceUrl + "/api/orders/validate")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(order)
                .retrieve()
                .bodyToMono(ValidationResponse.class)
                .timeout(timeout)
                .map(ValidationResponse::isValid)
                .doOnSuccess(result -> log.debug("Validation result for order {}: {}", order.getId(), result))
                .doOnError(
                        error -> log.error("Error validating order {}: {}", order.getId(), error.getMessage(), error))
                .onErrorReturn(false);
    }

    /**
     * Process an order with the external processing service
     *
     * @param order Order to process
     * @return Processed order or error
     */
    @CircuitBreaker(name = "externalServiceProcess")
    @Retry(name = "externalServiceProcess")
    @Observed(name = "external.service.process", contextualName = "processOrder")
    public Mono<Order> processOrder(Order order) {
        log.debug("Sending order for processing: {}", order.getId());

        return webClientBuilder
                .build()
                .post()
                .uri(serviceUrl + "/api/orders/process")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(order)
                .retrieve()
                .bodyToMono(Order.class)
                .timeout(timeout)
                .doOnSuccess(result -> log.debug("Order processed successfully: {}", result.getId()))
                .doOnError(
                        error -> log.error("Error processing order {}: {}", order.getId(), error.getMessage(), error));
    }

    /**
     * Notify about order completion
     *
     * @param order Completed order
     * @return Notification ID or error
     */
    @CircuitBreaker(name = "externalServiceNotify")
    @Retry(name = "externalServiceNotify")
    @Observed(name = "external.service.notify", contextualName = "notifyOrderComplete")
    public Mono<UUID> notifyOrderComplete(Order order) {
        log.debug("Sending completion notification for order: {}", order.getId());

        return webClientBuilder
                .build()
                .post()
                .uri(serviceUrl + "/api/orders/notify")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(order)
                .retrieve()
                .bodyToMono(NotificationResponse.class)
                .timeout(timeout)
                .map(NotificationResponse::getNotificationId)
                .doOnSuccess(result ->
                        log.debug("Notification sent for order: {}, notification ID: {}", order.getId(), result))
                .doOnError(error -> log.error(
                        "Error sending notification for order {}: {}", order.getId(), error.getMessage(), error));
    }

    @Data
    public static class ValidationResponse {
        private boolean valid;
        private String message;
    }

    @Data
    public static class NotificationResponse {
        private UUID notificationId;
        private String status;
        private String message;
    }
}