# Redis only — run from this directory or: make -C acme-infra/redis <target>
.DEFAULT_GOAL := help

INFRA_DIR := $(abspath $(dir $(lastword $(MAKEFILE_LIST)))/..)
COMPOSE := docker compose -f $(INFRA_DIR)/docker-compose.yml --project-directory $(INFRA_DIR)

REDIS_SERVICE_NAME := redis
REDIS_CONTAINER_NAME := acme-redis

.PHONY: help start stop logs status cli flush

help:
	@echo "Redis (shared container $(REDIS_CONTAINER_NAME), host port 6379)"
	@echo "  start    up -d redis only"
	@echo "  stop     stop redis service only"
	@echo "  logs     docker logs -f"
	@echo "  status   docker ps (filtered)"
	@echo "  cli      redis-cli inside container"
	@echo "  flush    FLUSHDB on database 0"
	@echo ""
	@echo "Compose file: $(INFRA_DIR)/docker-compose.yml"

start:
	@echo "Starting Redis..."
	@$(COMPOSE) up -d $(REDIS_SERVICE_NAME)
	@echo "Waiting for PING..."
	@timeout=30; \
	while [ $$timeout -gt 0 ]; do \
		if docker exec $(REDIS_CONTAINER_NAME) redis-cli ping 2>/dev/null | grep -q PONG; then \
			echo "Redis is ready."; \
			break; \
		fi; \
		sleep 1; \
		timeout=$$((timeout-1)); \
	done
	@docker exec $(REDIS_CONTAINER_NAME) redis-cli ping 2>/dev/null | grep -q PONG || \
		echo "Warning: still starting — try: make logs"

stop:
	@echo "Stopping Redis..."
	@$(COMPOSE) stop $(REDIS_SERVICE_NAME)
	@echo "Stopped."

logs:
	@docker logs -f $(REDIS_CONTAINER_NAME)

status:
	@docker ps -a | grep $(REDIS_CONTAINER_NAME) || echo "Container not found"

cli:
	@docker exec -it $(REDIS_CONTAINER_NAME) redis-cli

flush:
	@echo "FLUSHDB on $(REDIS_CONTAINER_NAME)..."
	@docker exec $(REDIS_CONTAINER_NAME) redis-cli FLUSHDB
	@echo "Done."
