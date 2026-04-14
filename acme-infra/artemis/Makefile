# Apache Artemis (AMQP/JMS) — optional profile. Run: make -C acme-infra/artemis <target>
.DEFAULT_GOAL := help

INFRA_DIR := $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/..)
COMPOSE := docker compose -f $(INFRA_DIR)/docker-compose.yml --project-directory $(INFRA_DIR) --profile artemis

ARTEMIS_CONTAINER_NAME := acme-artemis

.PHONY: help start stop logs status

help:
	@echo "Artemis (shared container $(ARTEMIS_CONTAINER_NAME))"
	@echo "  start    up -d artemis (core :61616, console :8161)"
	@echo "  stop     stop artemis service only"
	@echo "  logs     docker logs -f"
	@echo "  status   docker ps (filtered)"
	@echo ""
	@echo "Default credentials follow the image (override with ARTEMIS_USER / ARTEMIS_PASSWORD):"
	@echo "  user:     artemis"
	@echo "  password: artemis"
	@echo ""
	@echo "Compose file: $(INFRA_DIR)/docker-compose.yml (profile: artemis)"

start:
	@echo "Starting Artemis..."
	@$(COMPOSE) up -d artemis
	@echo "Broker :61616, web console http://localhost:8161"

stop:
	@echo "Stopping Artemis..."
	@$(COMPOSE) stop artemis
	@echo "Stopped."

logs:
	@docker logs -f $(ARTEMIS_CONTAINER_NAME)

status:
	@docker ps -a | grep $(ARTEMIS_CONTAINER_NAME) || echo "Container not found"
