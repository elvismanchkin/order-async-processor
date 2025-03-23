package dev.demo.order.async.processor.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Aspect that adds performance metrics for all service methods
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class MetricsAdvisor {

    private final MeterRegistry meterRegistry;

    @Around("execution(* dev.demo.order.async.processor.service.*.*(..))")
    public Object measureServiceMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getMethod().getName();

        // Create the timer
        Timer timer = meterRegistry.timer(
                "service.method.timer",
                "class", className,
                "method", methodName);

        // Start the timer
        long start = System.nanoTime();

        try {
            // Execute the method
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // Record exceptions
            meterRegistry.counter(
                    "service.method.errors",
                    "class", className,
                    "method", methodName,
                    "exception", throwable.getClass().getSimpleName()
            ).increment();
            throw throwable;
        } finally {
            // Record the timing
            timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }

    @Around("execution(* dev.demo.order.async.processor.repository.*.*(..))")
    public Object measureRepositoryMethods(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        String className = joinPoint.getTarget().getClass().getSimpleName();
        String methodName = signature.getMethod().getName();

        // Create the timer
        Timer timer = meterRegistry.timer(
                "repository.method.timer",
                "class", className,
                "method", methodName);

        // Start the timer
        long start = System.nanoTime();

        try {
            // Execute the method
            return joinPoint.proceed();
        } catch (Throwable throwable) {
            // Record exceptions
            meterRegistry.counter(
                    "repository.method.errors",
                    "class", className,
                    "method", methodName,
                    "exception", throwable.getClass().getSimpleName()
            ).increment();
            throw throwable;
        } finally {
            // Record the timing
            timer.record(System.nanoTime() - start, TimeUnit.NANOSECONDS);
        }
    }
}