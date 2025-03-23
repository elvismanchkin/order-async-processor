// New file: src/main/java/dev/demo/order/async/processor/MetricsThreadPoolTaskScheduler.java
package dev.demo.order.async.processor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tag;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.binder.jvm.ExecutorServiceMetrics;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

@Slf4j
public class MetricsThreadPoolTaskScheduler extends ThreadPoolTaskScheduler {

    private final MeterRegistry meterRegistry;
    private String metricsName = "scheduler";
    private Iterable<Tag> tags = Tags.empty();

    public MetricsThreadPoolTaskScheduler(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    protected ScheduledExecutorService createExecutor(int poolSize, ThreadFactory threadFactory,
                                                      RejectedExecutionHandler rejectedExecutionHandler) {
        ScheduledExecutorService executor = super.createExecutor(poolSize, threadFactory, rejectedExecutionHandler);
        return ExecutorServiceMetrics.monitor(meterRegistry, executor, metricsName, tags);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable task, java.util.Date startTime) {
        return instrumentedSchedule(() -> super.schedule(task, startTime), "schedule");
    }

    private <T> T instrumentedSchedule(Supplier<T> schedulingFunction, String operation) {
        long start = System.nanoTime();
        try {
            return schedulingFunction.get();
        } finally {
            long duration = System.nanoTime() - start;
            meterRegistry.timer("scheduler.operation",
                            "operation", operation)
                    .record(duration, TimeUnit.NANOSECONDS);
        }
    }
}