package dev.demo.order.async.processor;

import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;

import jakarta.annotation.PostConstruct;

@Configuration
public class MetricsConfig {

    private final ObservationRegistry observationRegistry;

    public MetricsConfig(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
    }

    @PostConstruct
    void setup() {
        Hooks.enableAutomaticContextPropagation();
    }

    @Bean
    public ObservedAspect observedAspect() {
        return new ObservedAspect(observationRegistry);
    }

    @Bean
    public ObservationThreadLocalAccessor observationThreadLocalAccessor() {
        return new ObservationThreadLocalAccessor();
    }
}