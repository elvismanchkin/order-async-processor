package dev.demo.order.async.processor;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.r2dbc.dialect.PostgresDialect;
import org.springframework.data.r2dbc.dialect.R2dbcDialect;

@Configuration
public class PostgresDialectConfig {

    @Bean
    public R2dbcDialect r2dbcDialect() {
        return PostgresDialect.INSTANCE;
    }
}
