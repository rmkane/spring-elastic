.DEFAULT_GOAL := help

# ============================================================================
# Variables
# ============================================================================

# Multi-module layout: API (Elasticsearch) :8885, consumer (gateway) :8883
DEBUG_PORT := 8787
MVN_DEBUG_OPTS := -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$(DEBUG_PORT)
# Root POM version only (-N); avoids full reactor scan for variable expansion
VERSION := $(shell mvn -N -q help:evaluate -Dexpression=project.version -DforceStdout)
API_ARTIFACT_ID := spring-elastic-api
CONSUMER_ARTIFACT_ID := spring-elastic-consumer
API_JAR := spring-elastic-api/target/$(API_ARTIFACT_ID)-$(VERSION).jar
CONSUMER_JAR := spring-elastic-consumer/target/$(CONSUMER_ARTIFACT_ID)-$(VERSION).jar

# Elasticsearch / Redis / Docker (see docker-compose.yml)
COMPOSE_FILE := docker-compose.yml
ES_CONTAINER_NAME := elasticsearch
REDIS_SERVICE_NAME := redis
REDIS_CONTAINER_NAME := spring-elastic-redis
ES_NETWORK := elastic-network
ES_PASSWORD_FILE := .es_password
ES_CERT_DIR := es-certs

# ============================================================================
# Phony targets
# ============================================================================

.PHONY: help clean build test \
	run-dev run-api run-consumer run-debug run-jar run-jar-consumer \
	stop-dev stop-jar status-dev status-jar \
	es-start es-stop es-logs es-status es-get-cert es-get-password es-setup es-clean \
	redis-start redis-stop redis-logs redis-status redis-cli redis-flush

# ============================================================================
# Help
# ============================================================================

help:
	@echo "Available targets:"
	@echo ""
	@echo "Application (API :8885 → Elasticsearch; consumer :8883 → API):"
	@echo "  clean              Remove Maven build output, $(ES_CERT_DIR)/, $(ES_PASSWORD_FILE)"
	@echo "  build              mvn package (all modules)"
	@echo "  test               mvn test (all modules)"
	@echo "  run-dev, run-api   spring-elastic-api via spring-boot:run (port 8885; start ES only)"
	@echo "  run-consumer       consumer gateway :8883 (start API first; start Redis for response caching)"
	@echo "  run-debug          API with JDWP on port $(DEBUG_PORT)"
	@echo "  run-jar            java -jar $(API_JAR) (run build first)"
	@echo "  run-jar-consumer   java -jar $(CONSUMER_JAR)"
	@echo "  stop-dev           pkill spring-boot:run"
	@echo "  stop-jar           pkill API/consumer executable JARs"
	@echo "  status-dev         pgrep spring-boot:run"
	@echo "  status-jar         pgrep API/consumer JAR processes"
	@echo ""
	@echo "Elasticsearch ($(COMPOSE_FILE)):"
	@echo "  es-start           docker compose up -d, wait for HTTPS :9200"
	@echo "  es-stop            docker compose stop"
	@echo "  es-logs            docker logs -f $(ES_CONTAINER_NAME)"
	@echo "  es-status          docker ps (filtered)"
	@echo "  es-get-cert        copy http CA cert → $(ES_CERT_DIR)/ca.crt"
	@echo "  es-get-password    write elastic password → $(ES_PASSWORD_FILE)"
	@echo "  es-setup           es-start, then cert + password (prints export hints)"
	@echo "  es-clean           compose down, remove container/network, local cert + password files"
	@echo ""
	@echo "Redis ($(COMPOSE_FILE) service $(REDIS_SERVICE_NAME), consumer gateway cache; host port 6379):"
	@echo "  redis-start        docker compose up -d redis, wait for PING"
	@echo "  redis-stop         docker compose stop redis (Elasticsearch keeps running)"
	@echo "  redis-logs         docker logs -f $(REDIS_CONTAINER_NAME)"
	@echo "  redis-status       docker ps (filtered)"
	@echo "  redis-cli          docker exec -it $(REDIS_CONTAINER_NAME) redis-cli"
	@echo "  redis-flush        FLUSHDB on Redis (clears consumer cache and anything else in DB 0)"

# ============================================================================
# Application
# ============================================================================

clean:
	@echo "Cleaning up..."
	@mvn clean
	@rm -rf $(ES_CERT_DIR) $(ES_PASSWORD_FILE)
	@echo "Cleanup complete."

build:
	@echo "Building the application..."
	@mvn package
	@echo "Build complete."

test:
	@echo "Running tests..."
	@mvn test
	@echo "Tests complete."

run-dev: run-api

run-api:
	@echo "Running Elasticsearch API ($(API_ARTIFACT_ID)) with DevTools on port 8885..."
	@mvn -pl $(API_ARTIFACT_ID) spring-boot:run
	@echo "API started."

run-consumer:
	@echo "Running consumer gateway ($(CONSUMER_ARTIFACT_ID)) on port 8883..."
	@mvn -pl $(CONSUMER_ARTIFACT_ID) spring-boot:run
	@echo "Consumer started."

run-debug:
	@echo "Running API in debug mode (JDWP $(DEBUG_PORT))..."
	@mvn -pl $(API_ARTIFACT_ID) spring-boot:run -Dspring-boot.run.jvmArguments="$(MVN_DEBUG_OPTS)"
	@echo "API started."

run-jar:
	@echo "Running API from JAR..."
	@java -jar $(API_JAR)
	@echo "API JAR started."

run-jar-consumer:
	@echo "Running consumer from JAR..."
	@java -jar $(CONSUMER_JAR)
	@echo "Consumer JAR started."

stop-dev:
	@echo "Stopping development server..."
	@pkill -f "spring-boot:run" || echo "No development server process found."

stop-jar:
	@echo "Stopping API / consumer JAR processes..."
	@pkill -f "$(API_ARTIFACT_ID)-.*\.jar" || true
	@pkill -f "$(CONSUMER_ARTIFACT_ID)-.*\.jar" || true
	@echo "Done."

status-dev:
	@echo "Checking development server status..."
	@if pgrep -f "spring-boot:run" > /dev/null; then \
		echo "✓ Development server is running"; \
		ps aux | grep -E "[s]pring-boot:run" | head -1; \
	else \
		echo "✗ Development server is not running"; \
	fi

status-jar:
	@echo "Checking API / consumer JAR status..."
	@if pgrep -f "$(API_ARTIFACT_ID)-.*\.jar" > /dev/null; then \
		echo "✓ API JAR is running"; \
		ps aux | grep -E "[j]ava.*$(API_ARTIFACT_ID).*\.jar" | head -1; \
	else \
		echo "✗ API JAR is not running"; \
	fi
	@if pgrep -f "$(CONSUMER_ARTIFACT_ID)-.*\.jar" > /dev/null; then \
		echo "✓ Consumer JAR is running"; \
		ps aux | grep -E "[j]ava.*$(CONSUMER_ARTIFACT_ID).*\.jar" | head -1; \
	else \
		echo "✗ Consumer JAR is not running"; \
	fi

# ============================================================================
# Elasticsearch
# ============================================================================

es-start:
	@echo "Starting Elasticsearch with Docker Compose..."
	@docker compose -f $(COMPOSE_FILE) up -d
	@echo "Waiting for Elasticsearch to be ready..."
	@timeout=120; \
	while [ $$timeout -gt 0 ]; do \
		if docker exec $(ES_CONTAINER_NAME) curl -s -k https://localhost:9200 > /dev/null 2>&1; then \
			echo "Elasticsearch is ready!"; \
			break; \
		fi; \
		sleep 3; \
		timeout=$$((timeout-3)); \
	done
	@if docker exec $(ES_CONTAINER_NAME) curl -s -k https://localhost:9200 > /dev/null 2>&1; then \
		echo "Elasticsearch container started successfully with HTTPS."; \
	else \
		echo "Warning: Elasticsearch may still be starting. Check logs with: make es-logs"; \
	fi
	@echo "Run 'make es-setup' to get cert and password."

es-stop:
	@echo "Stopping Elasticsearch..."
	@docker compose -f $(COMPOSE_FILE) stop
	@echo "Elasticsearch stopped."

es-logs:
	@docker logs -f $(ES_CONTAINER_NAME)

es-status:
	@docker ps -a | grep $(ES_CONTAINER_NAME) || echo "Container not found"

es-get-password:
	@echo "Extracting Elasticsearch password..."
	@docker exec $(ES_CONTAINER_NAME) /bin/bash -c "echo \$$ELASTIC_PASSWORD" 2>/dev/null | tr -d '\r\n' > $(ES_PASSWORD_FILE) || \
		(docker exec $(ES_CONTAINER_NAME) /bin/bash -c "bin/elasticsearch-reset-password -u elastic -b" 2>/dev/null | grep "New value:" | sed 's/.*New value: //' | tr -d '\r\n' > $(ES_PASSWORD_FILE))
	@if [ -s $(ES_PASSWORD_FILE) ]; then \
		echo "Password saved to $(ES_PASSWORD_FILE)"; \
		echo "Set environment variable: export ES_PASSWORD=$$(cat $(ES_PASSWORD_FILE))"; \
	else \
		echo "Warning: Could not extract password. You may need to reset it manually."; \
		echo "Try: docker exec -it $(ES_CONTAINER_NAME) bin/elasticsearch-reset-password -u elastic"; \
	fi

es-get-cert:
	@echo "Extracting Elasticsearch certificate..."
	@if ! docker ps | grep -q $(ES_CONTAINER_NAME); then \
		echo "Error: Container $(ES_CONTAINER_NAME) is not running. Start it with: make es-start"; \
		exit 1; \
	fi
	@mkdir -p $(ES_CERT_DIR)
	@echo "Waiting for certificates to be generated..."
	@timeout=60; \
	cert_found=0; \
	while [ $$timeout -gt 0 ] && [ $$cert_found -eq 0 ]; do \
		if docker exec $(ES_CONTAINER_NAME) test -f /usr/share/elasticsearch/config/certs/http_ca.crt 2>/dev/null; then \
			cert_found=1; \
			break; \
		fi; \
		sleep 2; \
		timeout=$$((timeout-2)); \
	done
	@if docker cp $(ES_CONTAINER_NAME):/usr/share/elasticsearch/config/certs/http_ca.crt $(ES_CERT_DIR)/ca.crt 2>/dev/null; then \
		echo "✓ Certificate saved to $(ES_CERT_DIR)/ca.crt"; \
		echo "Set environment variable: export ES_CERT_PATH=$$(pwd)/$(ES_CERT_DIR)/ca.crt"; \
	elif docker exec $(ES_CONTAINER_NAME) test -d /usr/share/elasticsearch/config/certs/ca 2>/dev/null && \
		docker cp $(ES_CONTAINER_NAME):/usr/share/elasticsearch/config/certs/ca/ca.crt $(ES_CERT_DIR)/ca.crt 2>/dev/null; then \
		echo "✓ Certificate saved to $(ES_CERT_DIR)/ca.crt"; \
		echo "Set environment variable: export ES_CERT_PATH=$$(pwd)/$(ES_CERT_DIR)/ca.crt"; \
	else \
		echo "Error: Could not find certificate file."; \
		echo "Checking available certificate files in container..."; \
		docker exec $(ES_CONTAINER_NAME) find /usr/share/elasticsearch/config/certs -name "*.crt" -o -name "*.pem" 2>/dev/null || \
		echo "No certificates found. Container may still be initializing. Try: make es-logs"; \
		exit 1; \
	fi
	@if [ -f $(ES_CERT_DIR)/ca.crt ]; then \
		echo "Certificate file size: $$(wc -c < $(ES_CERT_DIR)/ca.crt) bytes"; \
	fi

es-setup: es-start
	@echo "Waiting for Elasticsearch to fully initialize and generate certificates..."
	@sleep 30
	@$(MAKE) es-get-cert
	@$(MAKE) es-get-password
	@echo ""
	@echo "=========================================="
	@echo "Setup complete! Set these environment variables:"
	@if [ -f $(ES_PASSWORD_FILE) ]; then \
		echo "export ES_PASSWORD=$$(cat $(ES_PASSWORD_FILE))"; \
	else \
		echo "# Password file not found. Run: make es-get-password"; \
	fi
	@if [ -f $(ES_CERT_DIR)/ca.crt ]; then \
		echo "export ES_CERT_PATH=$$(pwd)/$(ES_CERT_DIR)/ca.crt"; \
	else \
		echo "# Certificate file not found. Run: make es-get-cert"; \
	fi
	@echo "export ES_URIS=https://localhost:9200"
	@echo "export ES_USERNAME=elastic"
	@echo "=========================================="

es-clean:
	@echo "Removing Elasticsearch stack..."
	@docker compose -f $(COMPOSE_FILE) down 2>/dev/null || true
	@docker stop $(ES_CONTAINER_NAME) 2>/dev/null || true
	@docker rm $(ES_CONTAINER_NAME) 2>/dev/null || true
	@docker stop $(REDIS_CONTAINER_NAME) 2>/dev/null || true
	@docker rm $(REDIS_CONTAINER_NAME) 2>/dev/null || true
	@docker network rm $(ES_NETWORK) 2>/dev/null || true
	@rm -rf $(ES_CERT_DIR) $(ES_PASSWORD_FILE)
	@echo "Cleanup complete."

# ============================================================================
# Redis
# ============================================================================

redis-start:
	@echo "Starting Redis ($(REDIS_SERVICE_NAME)) with Docker Compose..."
	@docker compose -f $(COMPOSE_FILE) up -d $(REDIS_SERVICE_NAME)
	@echo "Waiting for Redis to accept connections..."
	@timeout=30; \
	while [ $$timeout -gt 0 ]; do \
		if docker exec $(REDIS_CONTAINER_NAME) redis-cli ping 2>/dev/null | grep -q PONG; then \
			echo "Redis is ready (PONG)."; \
			break; \
		fi; \
		sleep 1; \
		timeout=$$((timeout-1)); \
	done
	@docker exec $(REDIS_CONTAINER_NAME) redis-cli ping 2>/dev/null | grep -q PONG || \
		(echo "Warning: Redis may still be starting. Check logs with: make redis-logs"; exit 0)

redis-stop:
	@echo "Stopping Redis..."
	@docker compose -f $(COMPOSE_FILE) stop $(REDIS_SERVICE_NAME)
	@echo "Redis stopped."

redis-logs:
	@docker logs -f $(REDIS_CONTAINER_NAME)

redis-status:
	@docker ps -a | grep $(REDIS_CONTAINER_NAME) || echo "Container not found"

redis-cli:
	@docker exec -it $(REDIS_CONTAINER_NAME) redis-cli

redis-flush:
	@echo "Flushing Redis database 0 ($(REDIS_CONTAINER_NAME))..."
	@docker exec $(REDIS_CONTAINER_NAME) redis-cli FLUSHDB
	@echo "Done."
