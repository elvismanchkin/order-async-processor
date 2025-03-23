package dev.demo.order.async.processor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ClassLoaderMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.UptimeMetrics;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.aop.ObservedAspect;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import reactor.core.publisher.Hooks;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
@Configuration
public class MetricsConfig {

    private final ObservationRegistry observationRegistry;
    private final MeterRegistry meterRegistry;
    @Value("${spring.application.name}")
    private String applicationName;
    private final Environment environment;

    @PostConstruct
    void setup() {
        Hooks.enableAutomaticContextPropagation();

        // Configure common tags for all metrics
        configureCommonTags();

        // Register JVM metrics
        new ClassLoaderMetrics().bindTo(meterRegistry);
        new JvmMemoryMetrics().bindTo(meterRegistry);
        new JvmGcMetrics().bindTo(meterRegistry);
        new JvmThreadMetrics().bindTo(meterRegistry);
        new ProcessorMetrics().bindTo(meterRegistry);
        new UptimeMetrics().bindTo(meterRegistry);

        log.info("Metrics configuration initialized");
    }

    private void configureCommonTags() {
        // Get active profiles
        List<String> profiles = List.of(environment.getActiveProfiles());
        String activeProfile = profiles.isEmpty() ? "default" : profiles.get(0);

        // Get hostname
        String hostname;
        try {
            hostname = InetAddress.getLocalHost().getHostName();
        } catch (UnknownHostException e) {
            hostname = "unknown";
            log.warn("Could not determine hostname for metrics", e);
        }

        // Add common tags to all metrics
        meterRegistry.config()
                .commonTags(Tags.of(
                        Tag.of("application", applicationName),
                        Tag.of("profile", activeProfile),
                        Tag.of("hostname", hostname)
                ));
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