package dev.demo.order.async.processor;

import io.micrometer.observation.ObservationRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;
import jakarta.annotation.PostConstruct;


@Configuration
@Slf4j
@ConditionalOnProperty(value = "spring.reactor.context-propagation.enabled", matchIfMissing = true)
public class ContextPropagationConfiguration {

    private final ObservationRegistry observationRegistry;

    public ContextPropagationConfiguration(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @PostConstruct
    void setupContextPropagation() {
        log.info("Setting up automatic context propagation hooks for reactive streams");
        Hooks.enableAutomaticContextPropagation();
    }
}