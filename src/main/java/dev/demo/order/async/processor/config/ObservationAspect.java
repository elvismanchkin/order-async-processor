package dev.demo.order.async.processor.config;

import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import io.micrometer.observation.annotation.Observed;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class ObservationAspect {

    private final ObservationRegistry observationRegistry;

    @Around("@annotation(io.micrometer.observation.annotation.Observed)")
    public Object trace(ProceedingJoinPoint joinPoint) throws Throwable {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();

        Observed observed = method.getAnnotation(Observed.class);
        String observationName = observed.name();
        String contextualName = observed.contextualName();

        Observation observation = Observation.createNotStarted(observationName, observationRegistry)
                .contextualName(contextualName)
                .lowCardinalityKeyValue("class", joinPoint.getTarget().getClass().getSimpleName())
                .lowCardinalityKeyValue("method", method.getName())
                .highCardinalityKeyValue("args", getArgsString(joinPoint));

        try {
            return observation.observe(() -> {
                try {
                    return joinPoint.proceed();
                } catch (Throwable e) {
                    throw new RuntimeException(e);
                }
            });
        } catch (Throwable throwable) {
            log.error("Error in observed method {}.{}: {}",
                    joinPoint.getTarget().getClass().getSimpleName(),
                    method.getName(),
                    throwable.getMessage());
            throw throwable;
        }
    }

    private String getArgsString(ProceedingJoinPoint joinPoint) {
        StringBuilder sb = new StringBuilder();
        Object[] args = joinPoint.getArgs();

        if (args.length == 0) {
            return "none";
        }

        for (int i = 0; i < args.length; i++) {
            Object arg = args[i];
            if (arg == null) {
                sb.append("null");
            } else if (arg instanceof String || arg instanceof Number || arg instanceof Boolean) {
                sb.append(arg);
            } else {
                sb.append(arg.getClass().getSimpleName());
            }
            if (i < args.length - 1) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }
}