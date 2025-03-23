package dev.demo.order.async.processor;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Tags;
import io.r2dbc.pool.ConnectionPool;
import io.r2dbc.pool.ConnectionPoolConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionConfiguration;
import io.r2dbc.postgresql.PostgresqlConnectionFactory;
import io.r2dbc.spi.ConnectionFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.r2dbc.config.AbstractR2dbcConfiguration;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.data.r2dbc.repository.config.EnableR2dbcRepositories;

import java.time.Duration;

@Configuration
@EnableR2dbcRepositories(basePackages = "dev.demo.order.async.processor.repository")
public class DatabaseConfig extends AbstractR2dbcConfiguration {

    @Value("${spring.r2dbc.host}")
    private String host;

    @Value("${spring.r2dbc.port}")
    private int port;

    @Value("${spring.r2dbc.database}")
    private String database;

    @Value("${spring.r2dbc.username}")
    private String username;

    @Value("${spring.r2dbc.password}")
    private String password;

    @Value("${spring.r2dbc.pool.initial-size:20}")
    private int initialSize;

    @Value("${spring.r2dbc.pool.max-size:100}")
    private int maxSize;

    @Value("${spring.r2dbc.pool.max-idle-time:30m}")
    private Duration maxIdleTime;

    @Value("${spring.r2dbc.pool.validation-query:SELECT 1}")
    private String validationQuery;

    @Value("${spring.r2dbc.pool.acquire-retry:3}")
    private int acquireRetry;

    private final MeterRegistry meterRegistry;

    @Autowired
    public DatabaseConfig(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Bean
    public PostgresqlConnectionFactory postgresqlConnectionFactory() {
        return new PostgresqlConnectionFactory(PostgresqlConnectionConfiguration.builder()
                .host(host)
                .port(port)
                .database(database)
                .username(username)
                .password(password)
                .schema("public")
                .connectTimeout(Duration.ofSeconds(5))
                .tcpKeepAlive(true)
                .tcpNoDelay(true)
                .build());
    }

    @Override
    @Bean
    @Primary
    public ConnectionFactory connectionFactory() {
        ConnectionPoolConfiguration configuration = ConnectionPoolConfiguration.builder(postgresqlConnectionFactory())
                .initialSize(initialSize)
                .maxSize(maxSize)
                .maxIdleTime(maxIdleTime)
                .acquireRetry(acquireRetry)
                .maxAcquireTime(Duration.ofSeconds(10))
                .validationQuery(validationQuery)
                // Register a custom metrics reporter
                .metricsRecorder(new MicrometerPoolMetricsRecorder(
                        meterRegistry,
                        "r2dbc.pool",
                        Tags.of("db", "postgres")
                ))
                .build();

        return new ConnectionPool(configuration);
    }

    @Bean
    public R2dbcEntityTemplate r2dbcEntityTemplate(ConnectionFactory connectionFactory) {
        return new R2dbcEntityTemplate(connectionFactory);
    }

    // Removed transactionManager bean - now provided by TransactionManagerConfiguration
}