## Project Structure

```
order-processor/
├── src/
│   ├── main/
│   │   ├── java/com/example/orderprocessor/
│   │   │   ├── client/             # External service clients
│   │   │   ├── config/             # Application configuration
│   │   │   ├── controller/         # REST API endpoints
│   │   │   ├── generator/          # Test data generation
│   │   │   ├── health/             # Health indicators
│   │   │   ├── model/              # Entity classes
│   │   │   ├── repository/         # Data access interfaces
│   │   │   ├── scheduler/          # Scheduled processors
│   │   │   ├── service/            # Business logic
│   │   │   │   └── impl/           # Service implementations
│   │   │   └── OrderProcessorApplication.java
│   │   └── resources/
│   │       ├── db/changelog/       # Liquibase migration files
│   │       │   ├── changes/        # Individual changeset files
│   │       │   │   ├── 001-initial-schema.xml
│   │       │   │   └── 002-test-data.xml
│   │       │   └── db.changelog-master.xml
│   │       ├── application.yml     # Main configuration
│   │       ├── application-docker.yml  # Docker profile config
│   │       └── application-datagen.yml # Data generation config
│   └── test/                       # Test classes
├── grafana/                        # Grafana configuration
│   ├── provisioning/
│   │   ├── datasources/            # Prometheus datasource
│   │   └── dashboards/             # Dashboard provisioning
│   └── dashboards/                 # Dashboard JSON definitions
├── prometheus/                     # Prometheus configuration
├── wiremock/                       # Mock external service
│   ├── mappings/                   # API endpoint definitions
│   └── __files/                    # Response template files
├── Dockerfile                      # Application container definition
├── docker-compose.yml              # Complete environment definition
├── pom.xml                         # Maven project definition
├── start.sh                        # Environment startup script
└── README.md                       # This file
```

## Getting Started

### Prerequisites

- Java 21 JDK
- Maven 3.8+
- Docker and Docker Compose
- Git

### Local Development Setup

1. Clone the repository:
   ```bash
   git clone https://github.com/your-org/order-processor.git
   cd order-processor
   ```

2. Build the application:
   ```bash
   ./mvnw clean package
   ```

3. Start the development environment:
   ```bash
   # Make the start script executable
   chmod +x start.sh
   
   # Run the start script which will setup all needed files
   ./start.sh
   ```

4. The script creates the following directory structure if it doesn't exist:
   ```
   ./prometheus/              # Prometheus configuration
   ./grafana/                 # Grafana configuration & dashboards
   ./wiremock/                # WireMock configuration
   ```

### Accessing Services

Once running, you can access:

- **Application API**: http://localhost:8080/api/orders
- **Application Metrics**: http://localhost:8080/actuator/prometheus
- **Prometheus**: http://localhost:9090
- **Grafana**: http://localhost:3000 (admin/admin)
- **PgAdmin**: http://localhost:5050 (admin@example.com/admin)
    - To connect to the database, add a new server with:
        - Host: postgres
        - Port: 5432
        - Database: orders_db
        - Username: postgres
        - Password: postgres
- **WireMock Admin Console**: http://localhost:8081/__admin

### Configuration Files

Key configuration files:

- **application.yml**: Main application configuration
    - Location: `src/main/resources/application.yml`
    - Contains database, server, metrics, and processing settings

- **application-docker.yml**: Docker environment settings
    - Location: `src/main/resources/application-docker.yml`
    - Settings applied when running with docker-compose

- **application-datagen.yml**: Data generation settings
    - Location: `src/main/resources/application-datagen.yml`
    - Configure test data generation frequency and batch size

- **docker-compose.yml**: Environment definition
    - Location: `./docker-compose.yml`
    - Defines all services and their connections

- **Liquibase Changelog**: Database schema definition
    - Location: `src/main/resources/db/changelog/db.changelog-master.xml`
    - Controls database migrations

## Customization

### Changing Data Generation Settings

Edit the `data.generator` section in `application-datagen.yml`:

```yaml
data:
  generator:
    enabled: true              # Enable/disable generation
    batch-size: 5              # Items per batch
    interval: 15000            # Milliseconds between batches
```

Or set environment variables in `docker-compose.yml`:

```yaml
environment:
  DATA_GENERATOR_ENABLED: "true"
  DATA_GENERATOR_INTERVAL: "15000"
```

### Adding Custom Dashboards

1. Create your dashboard JSON file in Grafana
2. Export it and save to `grafana/dashboards/`
3. Update `grafana/provisioning/dashboards/dashboards.yml` if needed

### Modifying Database Schema

1. Create a new XML file in `src/main/resources/db/changelog/changes/`
2. Add your changes using Liquibase XML format
3. Include the file in `db.changelog-master.xml`

## Troubleshooting

### Common Issues

1. **Connection refused to PostgreSQL**
    - Ensure PostgreSQL container is running: `docker ps`
    - Check logs: `docker-compose logs postgres`

2. **Data not being generated**
    - Verify the data generator is enabled in configuration
    - Check application logs: `docker-compose logs order-processor`

3. **Missing metrics in Grafana**
    - Ensure Prometheus is scraping metrics: http://localhost:9090/targets
    - Check datasource configuration in Grafana

4. **Container startup failures**
    - Stop all containers: `docker-compose down`
    - Remove volumes if needed: `docker-compose down -v`
    - Restart with: `docker-compose up -d`
    - Check logs: `docker-compose logs`

### Viewing Logs

```bash
# View logs for all services
docker-compose logs

# View logs for a specific service
docker-compose logs order-processor

# Follow logs in real-time
docker-compose logs -f

# View last 100 lines of logs
docker-compose logs --tail=100
```