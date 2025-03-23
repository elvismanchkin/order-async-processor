package dev.demo.order.async.processor.config;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import reactor.core.publisher.Mono;

/**
 * Error handler for WebClient requests
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class WebClientErrorHandler {

    private final MeterRegistry meterRegistry;

    /**
     * Creates a filter function for handling WebClient errors
     *
     * @return Filter function for WebClient
     */
    public ExchangeFilterFunction errorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(this::handleErrorResponse);
    }

    /**
     * Handles error responses from WebClient requests
     *
     * @param clientResponse Response from the client
     * @return Modified response or error
     */
    private Mono<ClientResponse> handleErrorResponse(ClientResponse clientResponse) {
        HttpStatusCode status = clientResponse.statusCode();

        if (status.is4xxClientError() || status.is5xxServerError()) {
            return clientResponse.bodyToMono(String.class)
                    .flatMap(errorBody -> {
                        String path = clientResponse.request() != null
                                ? clientResponse.request().getURI().getPath()
                                : "unknown";

                        log.error("WebClient error: {} {} - {}", status.value(), path, errorBody);

                        // Record metrics
                        meterRegistry.counter(
                                "webclient.error",
                                "status", String.valueOf(status.value()),
                                "path", path
                        ).increment();

                        if (status.is5xxServerError()) {
                            return Mono.error(new RuntimeException("Server error: " + status.value() + " " + errorBody));
                        } else {
                            return Mono.just(clientResponse);
                        }
                    });
        }

        return Mono.just(clientResponse);
    }
}