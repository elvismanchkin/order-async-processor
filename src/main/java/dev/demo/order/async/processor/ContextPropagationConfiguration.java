package dev.demo.order.async.processor;

import io.micrometer.context.ContextSnapshot;
import io.micrometer.context.ContextSnapshotFactory;
import io.micrometer.context.ContextRegistry;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.contextpropagation.ObservationThreadLocalAccessor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Operators;
import reactor.util.context.Context;
import jakarta.annotation.PostConstruct;

import java.util.concurrent.Callable;
import java.util.function.Function;

@Configuration
@Slf4j
public class ContextPropagationConfiguration {

    private final ObservationRegistry observationRegistry;
    private final ContextSnapshotFactory snapshotFactory;

    public ContextPropagationConfiguration(ObservationRegistry observationRegistry) {
        this.observationRegistry = observationRegistry;
        // Build a factory that captures values from all registered ThreadLocal accessors.
        this.snapshotFactory = ContextSnapshotFactory.builder().build();
    }

    @PostConstruct
    void setupContextPropagation() {
        ContextRegistry.getInstance().registerThreadLocalAccessor(
                new ObservationThreadLocalAccessor(observationRegistry)
        );

        log.info("Setting up context propagation hooks for reactive streams");

        Hooks.onEachOperator(
                ContextPropagationConfiguration.class.getSimpleName(),
                Operators.lift((scannable, subscriber) -> {
                    ContextSnapshot snapshot = snapshotFactory.captureAll();
                    return new ContextSnapshotSubscriber<>(subscriber, snapshot, Context.empty());
                })
        );
    }

    public static <T> Function<Context, T> withContext(Function<Context, T> fn) {
        // Build a factory for static context capture.
        ContextSnapshotFactory factory = ContextSnapshotFactory.builder().build();
        ContextSnapshot snapshot = factory.captureAll();
        return context -> {
            try {
                Callable<T> wrapped = snapshot.wrap(() -> fn.apply(context));
                return wrapped.call();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };
    }
}