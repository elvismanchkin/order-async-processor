package dev.demo.order.async.processor.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Global exception handler for REST controllers
 */
@RestControllerAdvice
@RequiredArgsConstructor
@Slf4j
public class GlobalExceptionHandler {

    private final MeterRegistry meterRegistry;

    /**
     * Handle ResponseStatusException
     */
    @ExceptionHandler(ResponseStatusException.class)
    public Mono<ResponseEntity<Map<String, Object>>> handleResponseStatusException(ResponseStatusException ex) {
        recordException(ex);

        HttpStatusCode status = ex.getStatusCode();
        String reason = ex.getReason();
        Map<String, Object> errorDetails = createErrorDetails(status, ex.getReason(), reason);

        return Mono.just(ResponseEntity.status(status).body(errorDetails));
    }

    /**
     * Handle IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public Mono<Map<String, Object>> handleIllegalArgumentException(IllegalArgumentException ex) {
        recordException(ex);
        return Mono.just(createErrorDetails(HttpStatus.BAD_REQUEST, ex.getMessage(), ex.getMessage()));
    }

    /**
     * Handle all other exceptions
     */
    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public Mono<Map<String, Object>> handleGeneralException(Exception ex) {
        recordException(ex);
        log.error("Unexpected error occurred: {}", ex.getMessage(), ex);
        return Mono.just(createErrorDetails(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", ex.getMessage()));
    }

    /**
     * Create a standardized error response
     */
    private Map<String, Object> createErrorDetails(HttpStatusCode status, String message, String reason) {
        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("timestamp", LocalDateTime.now().toString());
        errorDetails.put("status", status.value());
        errorDetails.put("error", reason);
        errorDetails.put("message", message);
        return errorDetails;
    }

    /**
     * Record exception in metrics
     */
    private void recordException(Exception ex) {
        meterRegistry.counter(
                "http.exceptions",
                "exception", ex.getClass().getSimpleName()
        ).increment();
    }
}