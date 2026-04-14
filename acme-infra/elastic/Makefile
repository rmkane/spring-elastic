# Elasticsearch only — run from this directory or: make -C acme-infra/elastic <target>
.DEFAULT_GOAL := help

INFRA_DIR := $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/..)
COMPOSE := docker compose -f $(INFRA_DIR)/docker-compose.yml --project-directory $(INFRA_DIR)

ES_CONTAINER_NAME := acme-elasticsearch
ES_NETWORK := acme-dev-network
# Same path as acme-dev-env.sh / ES_CERT_PATH (not under the repo)
ES_CERT_PATH := $(HOME)/.ssh/elastic-ca.crt

.PHONY: help start stop logs status get-cert setup clean

help:
	@echo "Elasticsearch (shared container $(ES_CONTAINER_NAME), compose project acme-infra)"
	@echo "  start        up -d elasticsearch only, wait for HTTPS :9200"
	@echo "  stop         stop elasticsearch service only"
	@echo "  logs         docker logs -f"
	@echo "  status       docker ps (filtered)"
	@echo "  get-cert     copy http CA → $(ES_CERT_PATH)"
	@echo "  setup        start, wait, extract CA (prints export hints)"
	@echo "  clean        stop+rm ES container; rm $(ES_CERT_PATH)"
	@echo ""
	@echo "Compose file: $(INFRA_DIR)/docker-compose.yml"

start:
	@echo "Starting Elasticsearch..."
	@$(COMPOSE) up -d elasticsearch
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
		echo "HTTPS :9200 is up."; \
	else \
		echo "Warning: still starting — try: make logs"; \
	fi
	@echo "Run 'make setup' or 'make get-cert' for the TLS CA. Use acme-dev-env.sh for ES_PASSWORD (default changeme unless you set ELASTIC_PASSWORD in .env)."

stop:
	@echo "Stopping Elasticsearch..."
	@$(COMPOSE) stop elasticsearch
	@echo "Stopped."

logs:
	@docker logs -f $(ES_CONTAINER_NAME)

status:
	@docker ps -a | grep $(ES_CONTAINER_NAME) || echo "Container not found"

get-cert:
	@echo "Extracting Elasticsearch CA certificate..."
	@if ! docker ps | grep -q $(ES_CONTAINER_NAME); then \
		echo "Error: $(ES_CONTAINER_NAME) is not running. Run: make start"; \
		exit 1; \
	fi
	@mkdir -p "$$HOME/.ssh"
	@echo "Waiting for certificates..."
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
	@if docker cp $(ES_CONTAINER_NAME):/usr/share/elasticsearch/config/certs/http_ca.crt "$(ES_CERT_PATH)" 2>/dev/null; then \
		echo "Certificate saved to $(ES_CERT_PATH)"; \
		echo "export ES_CERT_PATH=$(ES_CERT_PATH)"; \
	elif docker exec $(ES_CONTAINER_NAME) test -d /usr/share/elasticsearch/config/certs/ca 2>/dev/null && \
		docker cp $(ES_CONTAINER_NAME):/usr/share/elasticsearch/config/certs/ca/ca.crt "$(ES_CERT_PATH)" 2>/dev/null; then \
		echo "Certificate saved to $(ES_CERT_PATH)"; \
		echo "export ES_CERT_PATH=$(ES_CERT_PATH)"; \
	else \
		echo "Error: could not find certificate in container."; \
		docker exec $(ES_CONTAINER_NAME) find /usr/share/elasticsearch/config/certs -name "*.crt" -o -name "*.pem" 2>/dev/null || true; \
		exit 1; \
	fi
	@chmod 644 "$(ES_CERT_PATH)"

setup: start
	@echo "Waiting for Elasticsearch to finish initializing..."
	@sleep 30
	@$(MAKE) get-cert
	@echo ""
	@echo "=========================================="
	@echo "Use acme-infra/acme-dev-env.sh for ES_* (password defaults to changeme; override ES_PASSWORD if needed)."
	@if [ -f "$(ES_CERT_PATH)" ]; then \
		echo "export ES_CERT_PATH=$(ES_CERT_PATH)"; \
	fi
	@echo "export ES_URIS=https://localhost:9200"
	@echo "export ES_USERNAME=elastic"
	@echo "=========================================="

clean:
	@echo "Stopping and removing Elasticsearch container (other acme-infra services unchanged)..."
	@$(COMPOSE) stop elasticsearch 2>/dev/null || true
	@$(COMPOSE) rm -f elasticsearch 2>/dev/null || true
	@rm -f "$(ES_CERT_PATH)"
	@echo "Done."
