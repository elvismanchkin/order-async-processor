package dev.demo.order.async.processor;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.Timer;
import reactor.pool.PoolMetricsRecorder;

import java.util.concurrent.TimeUnit;

public class MicrometerPoolMetricsRecorder implements PoolMetricsRecorder {
    private final MeterRegistry meterRegistry;
    private final String prefix;
    private final Tags tags;

    public MicrometerPoolMetricsRecorder(MeterRegistry meterRegistry, String prefix, Tags tags) {
        this.meterRegistry = meterRegistry;
        this.prefix = prefix;
        this.tags = tags;
    }

    @Override
    public void recordAllocationSuccessAndLatency(long latencyNanos) {
        Timer.builder(prefix + ".allocation.success")
                .tags(tags)
                .register(meterRegistry)
                .record(latencyNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordAllocationFailureAndLatency(long latencyNanos) {
        Timer.builder(prefix + ".allocation.failure")
                .tags(tags)
                .register(meterRegistry)
                .record(latencyNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordResetLatency(long latencyNanos) {
        Timer.builder(prefix + ".reset").tags(tags).register(meterRegistry).record(latencyNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordDestroyLatency(long latencyNanos) {
        Timer.builder(prefix + ".destroy")
                .tags(tags)
                .register(meterRegistry)
                .record(latencyNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordRecycled() {
        Counter.builder(prefix + ".recycled").tags(tags).register(meterRegistry).increment();
    }

    @Override
    public void recordLifetimeDuration(long durationNanos) {
        Timer.builder(prefix + ".lifetime")
                .tags(tags)
                .register(meterRegistry)
                .record(durationNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordIdleTime(long idleTimeNanos) {
        Timer.builder(prefix + ".idle.time")
                .tags(tags)
                .register(meterRegistry)
                .record(idleTimeNanos, TimeUnit.NANOSECONDS);
    }

    @Override
    public void recordSlowPath() {
        Counter.builder(prefix + ".acquire.slow")
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }

    @Override
    public void recordFastPath() {
        Counter.builder(prefix + ".acquire.fast")
                .tags(tags)
                .register(meterRegistry)
                .increment();
    }
}