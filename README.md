# Order Processor Service

A Spring Boot application for processing orders using reactive programming and advanced scheduling capabilities.

## Features

- Reactive processing using Spring WebFlux and R2DBC
- Scheduled batch processing of orders
- Context propagation with micrometer for observability
- Resilience with circuit breakers, retries, and timeouts
- Metrics collection and monitoring
- Database connection pooling for high throughput
- Integration with external services
- Containerization support

## Tech Stack

- Java 21
- Spring Boot 3
- Spring WebFlux
- Spring Data R2DBC
- PostgreSQL
- Reactor Core
- Micrometer for metrics and tracing
- Resilience4j for fault tolerance
- Docker and Docker Compose

## Architecture

The application is designed as a reactive service that periodically fetches orders from a PostgreSQL database using
R2DBC, processes them through one or more external services, and updates their status in the database.

Key components:

- `OrderProcessorScheduler`: Manages the scheduled execution of order processing
- `OrderService`: Business logic for working with orders
- `OrderRepository`: Data access layer using R2DBC
- `ExternalServiceClient`: Client for calling external services
- Metrics configuration for observability
- Resilience configuration for fault tolerance

## Getting Started

### Prerequisites

- Java 21 or higher
- Maven
- Docker and Docker Compose (for containerized execution)
- PostgreSQL (if running locally without Docker)

### Running Locally

1. Clone the repository

```
git clone https://github.com/your-org/order-processor.git
cd order-processor
```

2. Build the application

```
./mvnw clean package
```

3. Start the application with Docker Compose

```
docker-compose up -d
```

4. Access the application

```
http://localhost:8080/actuator/health
```

### Configuration

The application is configured via `application.yml`. Key configuration options:

- `order.processing.batch-size`: Number of orders to process in each batch
- `order.processing.concurrency`: Number of concurrent order processing operations
- `order.processing.interval`: Time interval between batch processing runs
- `order.processing.types`: Types of orders to process
- `spring.r2dbc.pool.*`: Database connection pool settings

### Monitoring

The application exposes metrics through Spring Boot Actuator and Prometheus:

- Health check: `http://localhost:8080/actuator/health`
- Metrics: `http://localhost:8080/actuator/prometheus`
- Prometheus UI: `http://localhost:9090`
- Grafana dashboards: `http://localhost:3000` (admin/admin)

## Database Schema

The application uses the following database tables:

- `orders`: Main orders table
- `orders_actions`: Actions performed on orders
- `orders_documents`: Documents associated with orders
- `orders_communications`: Communications related to orders

## Performance Tuning

For high-throughput environments:

1. Adjust the R2DBC connection pool settings in `application.yml`:

```yaml
spring:
  r2dbc:
    pool:
      initial-size: 100  # Increase for higher load
      max-size: 500      # Adjust based on available resources
```

2. Tune the batch processing parameters:

```yaml
order:
  processing:
    batch-size: 500      # Increase for higher throughput
    concurrency: 50      # Adjust based on CPU cores and memory
```

3. JVM settings (set in `JAVA_OPTS` environment variable):

```
-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0 -XX:+OptimizeStringConcat -XX:+UseStringDeduplication
```

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-feature`
3. Commit your changes: `git commit -m 'Add new feature'`
4. Push to the branch: `git push origin feature/new-feature`
5. Submit a pull request

## License

This project is licensed under the MIT License - see the LICENSE file for details.