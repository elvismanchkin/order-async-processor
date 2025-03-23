package dev.demo.order.async.processor.retry;

import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.RetryRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Component
@RequiredArgsConstructor
@Slf4j
public class RetryableService {

    private final CircuitBreakerRegistry circuitBreakerRegistry;
    private final RetryRegistry retryRegistry;

    public <T> Mono<T> executeWithRetry(String operationName, Mono<T> operation) {
        return operation
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker(operationName)))
                .transformDeferred(RetryOperator.of(retryRegistry.retry(operationName)))
                .doOnError(e -> log.error("Operation {} failed after retries: {}", operationName, e.getMessage(), e));
    }

    public <T> Flux<T> executeFluxWithRetry(String operationName, Flux<T> operation) {
        return operation
                .transformDeferred(CircuitBreakerOperator.of(circuitBreakerRegistry.circuitBreaker(operationName)))
                .transformDeferred(RetryOperator.of(retryRegistry.retry(operationName)))
                .doOnError(e -> log.error("Operation {} failed after retries: {}", operationName, e.getMessage(), e));
    }
}