#!/bin/bash

# Create directories for grafana and prometheus
mkdir -p prometheus grafana/provisioning/datasources grafana/provisioning/dashboards grafana/dashboards wiremock/mappings wiremock/__files

# Check if files exist and copy them if not
[ ! -f prometheus/prometheus.yml ] && echo "Creating prometheus config..." && cp -n prometheus-config.yml prometheus/prometheus.yml
[ ! -f grafana/provisioning/datasources/datasource.yml ] && echo "Creating Grafana datasource..." && cp -n grafana-datasource.yml grafana/provisioning/datasources/datasource.yml
[ ! -f grafana/provisioning/dashboards/dashboards.yml ] && echo "Creating Grafana dashboard provisioning..." && cp -n grafana-dashboard-provision.yml grafana/provisioning/dashboards/dashboards.yml
[ ! -f grafana/dashboards/order-processor-dashboard.json ] && echo "Creating Grafana dashboard..." && cp -n metrics-dashboard.json grafana/dashboards/order-processor-dashboard.json
[ ! -f wiremock/mappings/external-service.json ] && echo "Creating WireMock mappings..." && cp -n wiremock-mappings.json wiremock/mappings/external-service.json
[ ! -f wiremock/__files/order-response.json ] && echo "Creating WireMock responses..." && cp -n wiremock-response.json wiremock/__files/order-response.json

# Start the services
echo "Starting services with Docker Compose..."
docker-compose up -d

echo "Services are starting. You can access:"
echo "- Application API: http://localhost:8080"
echo "- Application metrics: http://localhost:8080/actuator/prometheus"
echo "- Prometheus: http://localhost:9090"
echo "- Grafana: http://localhost:3000 (admin/admin)"
echo "- PgAdmin: http://localhost:5050 (admin@example.com/admin)"
echo "- WireMock: http://localhost:8081/__admin"

echo "To view logs: docker-compose logs -f"