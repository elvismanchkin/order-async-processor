package dev.demo.order.async.processor.health;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.r2dbc.core.DatabaseClient;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Component
@RequiredArgsConstructor
@Slf4j
public class DatabaseHealthIndicator implements ReactiveHealthIndicator {

    private final DatabaseClient databaseClient;

    @Override
    public Mono<Health> health() {
        return checkConnection()
                .timeout(Duration.ofSeconds(5))
                .map(result -> Health.up()
                        .withDetail("database", "PostgreSQL")
                        .withDetail("result", result)
                        .build())
                .onErrorResume(e -> {
                    log.error("Database health check failed", e);
                    return Mono.just(Health.down()
                            .withDetail("database", "PostgreSQL")
                            .withDetail("error", e.getMessage())
                            .build());
                });
    }

    private Mono<String> checkConnection() {
        return databaseClient.sql("SELECT 1")
                .map(row -> "Connected successfully")
                .first();
    }
}