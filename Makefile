.PHONY: help es-start es-stop es-logs es-get-cert es-get-password es-status

# Elasticsearch container name
ES_CONTAINER_NAME := elasticsearch
ES_NETWORK := elastic-network
ES_PASSWORD_FILE := .es_password
ES_CERT_DIR := es-certs

help:
	@echo "Available targets:"
	@echo "  es-start        - Start Elasticsearch Docker container"
	@echo "  es-stop         - Stop Elasticsearch Docker container"
	@echo "  es-logs         - Show Elasticsearch container logs"
	@echo "  es-get-cert     - Extract certificate from container to es-certs/"
	@echo "  es-get-password - Extract password from container to .es_password"
	@echo "  es-status       - Check Elasticsearch container status"
	@echo "  es-setup        - Complete setup: start ES, get cert and password"

es-start:
	@echo "Starting Elasticsearch container..."
	@docker network create $(ES_NETWORK) 2>/dev/null || true
	@if docker ps -a | grep -q $(ES_CONTAINER_NAME); then \
		echo "Container exists, starting it..."; \
		docker start $(ES_CONTAINER_NAME); \
	else \
		echo "Creating new container..."; \
		docker run -d \
			--name $(ES_CONTAINER_NAME) \
			--network $(ES_NETWORK) \
			-p 9200:9200 \
			-p 9300:9300 \
			-e "discovery.type=single-node" \
			-e "ELASTIC_PASSWORD=$$(openssl rand -base64 32 | tr -d '\n')" \
			docker.elastic.co/elasticsearch/elasticsearch:8.11.0; \
	fi
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
	@echo "Stopping Elasticsearch container..."
	@docker stop $(ES_CONTAINER_NAME) || true
	@echo "Elasticsearch container stopped."

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
	@echo "Removing Elasticsearch container and network..."
	@docker stop $(ES_CONTAINER_NAME) 2>/dev/null || true
	@docker rm $(ES_CONTAINER_NAME) 2>/dev/null || true
	@docker network rm $(ES_NETWORK) 2>/dev/null || true
	@docker volume rm es-certs-data 2>/dev/null || true
	@rm -rf $(ES_CERT_DIR) $(ES_PASSWORD_FILE)
	@echo "Cleanup complete."

