package dev.demo.order.async.processor;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableScheduling
@EnableAsync
@SpringBootApplication
public class OrderAsyncProcessorApplication {

    public static void main(String[] args) {
        SpringApplication.run(OrderAsyncProcessorApplication.class, args);
    }
}
