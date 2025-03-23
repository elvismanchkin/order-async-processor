# Order Processing Service

A reactive Spring Boot application for processing orders, documents, and communications with high performance at scale.

## Features

- Reactive programming with Spring WebFlux and R2DBC
- Scheduled batch processing of orders, documents, and communications
- Advanced metrics and tracing with context propagation
- Circuit breakers and retry mechanisms for resilience
- Optimized for high throughput with connection pooling
- Comprehensive API for order management and processing

## Tech Stack

- Java 21
- Spring Boot 3
- Spring WebFlux and Project Reactor
- R2DBC with PostgreSQL for non-blocking database access
- Micrometer for metrics and observability
- Resilience4j for fault tolerance

## Getting Started

### Prerequisites

- Java 21
- Docker and Docker Compose
- PostgreSQL (when running locally without Docker)

### Running with Docker Compose

```bash
docker-compose up -d
```

This will start the application along with PostgreSQL, Prometheus for metrics, and Grafana for dashboards.

### Building manually

```bash
./mvnw clean package
java -jar target/order-processor-0.0.1-SNAPSHOT.jar
```

## API Endpoints

The application provides RESTful APIs for managing:

- **Orders**: Creation, updates, processing, and deletion
- **Customers**: Customer management and lookup
- **Documents**: Document uploads, management, and processing
- **Communications**: Customer communications and notifications

## Configuration

Key configuration properties in `application.yml`:

```yaml
# Order Processing
order:
  processing:
    batch-size: 200            # Orders per batch
    concurrency: 20            # Concurrent operations
    interval: 60000            # Scheduler interval in ms
    types: STANDARD,PRIORITY   # Order types to process

# Database Configuration
spring:
  r2dbc:
    pool:
      initial-size: 50
      max-size: 200
      max-idle-time: 30m
```

## Monitoring

The application exposes metrics through Spring Boot Actuator:

- **Health check**: `/actuator/health`
- **Metrics**: `/actuator/prometheus`
- **Grafana dashboard**: Available on port 3000

## Performance Tuning

For high-throughput environments:

1. Increase the R2DBC connection pool size:
   ```yaml
   spring:
     r2dbc:
       pool:
         max-size: 300  # Default: 200
   ```

2. Adjust batch processing parameters:
   ```yaml
   order:
     processing:
       batch-size: 500     # Default: 200
       concurrency: 50     # Default: 20
   ```

3. Optimize JVM settings:
   ```
   -XX:+UseG1GC -Xmx4g -XX:+UseStringDeduplication -XX:+OptimizeStringConcat
   ```

## Project Structure

- `model`: Entity classes
- `repository`: Data access interfaces
- `service`: Business logic implementation
- `controller`: REST API endpoints
- `config`: Application configuration
- `scheduler`: Scheduled processors

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/new-feature`
3. Run tests: `./mvnw verify`
4. Commit changes: `git commit -m 'Add new feature'`
5. Push to the branch: `git push origin feature/new-feature`
6. Submit a pull request