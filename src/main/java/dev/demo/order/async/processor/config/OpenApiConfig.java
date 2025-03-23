package dev.demo.order.async.processor.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI orderProcessorOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Order Processor API")
                        .description("Reactive API for processing orders, documents, and communications")
                        .version("v1.0")
                        .contact(new Contact()
                                .name("Development Team")
                                .email("dev@example.com"))
                        .license(new License()
                                .name("MIT License")
                                .url("https://opensource.org/licenses/MIT")))
                .servers(List.of(
                        new Server().url("/").description("Default Server URL"),
                        new Server().url("http://localhost:8080").description("Local Development")
                ));
    }
}