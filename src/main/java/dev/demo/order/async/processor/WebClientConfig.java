package dev.demo.order.async.processor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.observation.ObservationRegistry;
import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.reactive.function.client.WebClientCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClient;
import reactor.netty.resources.ConnectionProvider;

import java.net.URI;
import java.time.Duration;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;

@Configuration
@Slf4j
public class WebClientConfig {

    @Value("${webclient.timeout.connect:5000}")
    private int connectTimeout;

    @Value("${webclient.timeout.read:30000}")
    private int readTimeout;

    @Value("${webclient.timeout.write:30000}")
    private int writeTimeout;

    @Value("${webclient.max-connections:500}")
    private int maxConnections;

    @Value("${webclient.acquire-timeout:45000}")
    private int acquireTimeout;

    @Bean
    public WebClient.Builder webClientBuilder(
            ObservationRegistry observationRegistry,
            MeterRegistry meterRegistry
    ) {
        ConnectionProvider provider = ConnectionProvider.builder("custom")
                .maxConnections(maxConnections)
                .pendingAcquireTimeout(Duration.ofMillis(acquireTimeout))
                .maxIdleTime(Duration.ofSeconds(60))
                .build();

        HttpClient httpClient = HttpClient.create(provider)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, connectTimeout)
                .responseTimeout(Duration.ofMillis(readTimeout))
                .doOnConnected(conn -> conn.addHandlerLast(new ReadTimeoutHandler(readTimeout, TimeUnit.MILLISECONDS))
                        .addHandlerLast(new WriteTimeoutHandler(writeTimeout, TimeUnit.MILLISECONDS)));

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .filter(logRequest())
                .filter(metricsFilter(meterRegistry))
                .filter(errorHandlingFilter())
                .observationRegistry(observationRegistry);
    }

    @Bean
    public WebClientCustomizer webClientCustomizer() {
        return webClientBuilder -> webClientBuilder
                .defaultHeader("User-Agent", "OrderProcessor/1.0");
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(request -> {
            if (log.isDebugEnabled()) {
                log.debug("Request: {} {}", request.method(), request.url());
            }
            return Mono.just(ClientRequest.from(request).build());
        });
    }

    private ExchangeFilterFunction metricsFilter(MeterRegistry meterRegistry) {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            String host = response.request().getURI().getHost();
            String path = response.request().getURI().getPath();
            String method = response.request().getMethod().name();
            int status = response.statusCode().value();

            meterRegistry.timer(
                    "webclient.request",
                    Arrays.asList(
                            Tag.of("method", method),
                            Tag.of("host", host),
                            Tag.of("path", path),
                            Tag.of("status", Integer.toString(status))
                    )
            ).record(Duration.ofMillis(1));

            return Mono.just(response);
        });
    }

    private ExchangeFilterFunction errorHandlingFilter() {
        return ExchangeFilterFunction.ofResponseProcessor(response -> {
            HttpStatusCode status = response.statusCode();
            if (status.is4xxClientError() || status.is5xxServerError()) {
                return response.bodyToMono(String.class)
                    .defaultIfEmpty("[no body]")
                    .flatMap(body -> {
                        URI url = response.request().getURI();
                        log.warn("Error response: {} {} - {}",
                            status.value(), url, body);
                            
                        if (status.is5xxServerError()) {
                            return Mono.error(new WebClientServerException(
                                "Server error: " + status.value(), 
                                url, 
                                status.value(), 
                                body));
                        }
                        return Mono.just(response);
                    });
            }
            return Mono.just(response);
        });
    }

    @EqualsAndHashCode(callSuper = true)
    @Data
    public static class WebClientServerException extends RuntimeException {
        private final URI url;
        private final int statusCode;
        private final String responseBody;

        public WebClientServerException(String message, URI url, int statusCode, String responseBody) {
            super(message);
            this.url = url;
            this.statusCode = statusCode;
            this.responseBody = responseBody;
        }

        // Getters omitted
    }
}