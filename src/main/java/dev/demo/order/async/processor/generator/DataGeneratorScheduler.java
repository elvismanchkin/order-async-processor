// src/main/java/dev/demo/order/async/processor/generator/DataGeneratorScheduler.java

package dev.demo.order.async.processor.generator;

import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataGeneratorScheduler {

    private final DataGeneratorService dataGeneratorService;
    private final RetryableDataGeneratorService retryableDataGeneratorService;

    @Value("${data.generator.enabled:false}")
    private boolean enabled;

    @Scheduled(fixedDelayString = "${data.generator.interval:30000}")
    @Observed(name = "data.generator.scheduled", contextualName = "scheduledDataGeneration")
    public void generateData() {
        if (!enabled) {
            log.debug("Data generation is disabled");
            return;
        }

        log.info("Starting scheduled data generation");

        // Use the retryable service instead
        retryableDataGeneratorService
                .generateBatchData()
                .doOnSuccess(v -> log.info("Completed scheduled data generation"))
                .doOnError(error -> log.error("Error during data generation: {}", error.getMessage(), error))
                .subscribe();
    }
}