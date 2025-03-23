package dev.demo.order.async.processor;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.test.context.TestPropertySource;

@TestConfiguration
@TestPropertySource(properties = {"spring.main.allow-bean-definition-overriding=true"})
public class TestConfig {
    // No additional beans needed
}
