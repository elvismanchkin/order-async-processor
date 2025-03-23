package dev.demo.order.async.processor;

import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
@Slf4j
@EnableScheduling
public class SchedulerConfig {

    @Value("${scheduler.pool-size:5}")
    private int poolSize;

    @Value("${scheduler.thread-name-prefix:OrderScheduler-}")
    private String threadNamePrefix;

    @Value("${scheduler.await-termination-seconds:60}")
    private int awaitTerminationSeconds;

    @Value("${scheduler.wait-for-tasks-on-shutdown:true}")
    private boolean waitForTasksToCompleteOnShutdown;

    @Bean
    public ThreadPoolTaskScheduler threadPoolTaskScheduler(MeterRegistry meterRegistry) {
        MetricsThreadPoolTaskScheduler scheduler = new MetricsThreadPoolTaskScheduler(meterRegistry);
        scheduler.setPoolSize(poolSize);
        scheduler.setThreadNamePrefix(threadNamePrefix);
        scheduler.setAwaitTerminationSeconds(awaitTerminationSeconds);
        scheduler.setWaitForTasksToCompleteOnShutdown(waitForTasksToCompleteOnShutdown);
        
        // Handle errors in a more resilient way
        scheduler.setErrorHandler(throwable -> {
            log.error("Unhandled exception in scheduler task", throwable);
        });
        
        // Configure rejection policy
        scheduler.setRejectedExecutionHandler((runnable, executor) -> {
            log.warn("Task rejected from scheduler pool, using CALLER_RUNS policy");
            
            if (!executor.isShutdown()) {
                runnable.run();
            }
        });
        
        return scheduler;
    }
}