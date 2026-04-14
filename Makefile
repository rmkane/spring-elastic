.DEFAULT_GOAL := help

# ============================================================================
# Variables
# ============================================================================

DEBUG_PORT := 8787
MVN_DEBUG_OPTS := -agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=$(DEBUG_PORT)
VERSION := $(shell mvn -N -q help:evaluate -Dexpression=project.version -DforceStdout)
API_ARTIFACT_ID := acme-elastic-api
CONSUMER_ARTIFACT_ID := acme-elastic-consumer
API_JAR := acme-elastic-api/target/$(API_ARTIFACT_ID)-$(VERSION).jar
CONSUMER_JAR := acme-elastic-consumer/target/$(CONSUMER_ARTIFACT_ID)-$(VERSION).jar

# Shared containers live in acme-infra/ (not a Maven module)
ACME_INFRA := acme-infra

# ============================================================================
# Phony targets
# ============================================================================

.PHONY: help clean build test lint format \
	run-dev run-api run-consumer run-debug run-jar run-jar-consumer \
	stop-dev stop-jar status-dev status-jar \
	es-start es-stop es-logs es-status es-get-cert es-setup es-clean \
	redis-start redis-stop redis-logs redis-status redis-cli redis-flush \
	artemis-start artemis-stop artemis-logs artemis-status \
	infra-help

# ============================================================================
# Help
# ============================================================================

help:
	@echo "Available targets:"
	@echo ""
	@echo "Application (API :8885 → Elasticsearch; consumer :8883 → API + Redis cache):"
	@echo "  clean              mvn clean (Elasticsearch CA is under ~/.ssh, not the repo)"
	@echo "  build              mvn package (all modules)"
	@echo "  test               mvn test (all modules)"
	@echo "  lint               mvn spotless:check (formatting / imports)"
	@echo "  format             mvn spotless:apply (write Eclipse formatter + import order)"
	@echo "  run-dev, run-api   $(API_ARTIFACT_ID) via spring-boot:run (port 8885)"
	@echo "  run-consumer       $(CONSUMER_ARTIFACT_ID) :8883 (API up; Redis optional for cache)"
	@echo "  run-debug          API with JDWP on port $(DEBUG_PORT)"
	@echo "  run-jar            java -jar $(API_JAR) (run build first)"
	@echo "  run-jar-consumer   java -jar $(CONSUMER_JAR)"
	@echo "  stop-dev           pkill spring-boot:run"
	@echo "  stop-jar           pkill API/consumer executable JARs"
	@echo "  status-dev         pgrep spring-boot:run"
	@echo "  status-jar         pgrep API/consumer JAR processes"
	@echo ""
	@echo "Shared containers ($(ACME_INFRA)/ — use from any checkout path):"
	@echo "  infra-help         summary of per-service Makefiles under $(ACME_INFRA)/"
	@echo "  es-start … es-clean   → make -C $(ACME_INFRA)/elastic … (CA → \$$HOME/.ssh/elastic-ca.crt)"
	@echo "  redis-start … redis-flush → make -C $(ACME_INFRA)/redis …"
	@echo "  artemis-start … artemis-status → make -C $(ACME_INFRA)/artemis …"

infra-help:
	@$(MAKE) -C $(ACME_INFRA) help

# ============================================================================
# Application
# ============================================================================

clean:
	@echo "Cleaning up..."
	@mvn clean
	@echo "Cleanup complete."

build:
	@echo "Building the application..."
	@mvn package
	@echo "Build complete."

test:
	@echo "Running tests..."
	@mvn test
	@echo "Tests complete."

lint:
	@echo "Running Spotless check..."
	@mvn spotless:check
	@echo "Lint complete."

format:
	@echo "Applying Spotless (formatter.xml)..."
	@mvn spotless:apply
	@echo "Format complete."

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
# Elasticsearch (delegates to acme-infra/elastic)
# ============================================================================

es-start:
	@$(MAKE) -C $(ACME_INFRA)/elastic start

es-stop:
	@$(MAKE) -C $(ACME_INFRA)/elastic stop

es-logs:
	@$(MAKE) -C $(ACME_INFRA)/elastic logs

es-status:
	@$(MAKE) -C $(ACME_INFRA)/elastic status

es-get-cert:
	@$(MAKE) -C $(ACME_INFRA)/elastic get-cert

es-setup:
	@$(MAKE) -C $(ACME_INFRA)/elastic setup

es-clean:
	@$(MAKE) -C $(ACME_INFRA)/elastic clean

# ============================================================================
# Redis (delegates to acme-infra/redis)
# ============================================================================

redis-start:
	@$(MAKE) -C $(ACME_INFRA)/redis start

redis-stop:
	@$(MAKE) -C $(ACME_INFRA)/redis stop

redis-logs:
	@$(MAKE) -C $(ACME_INFRA)/redis logs

redis-status:
	@$(MAKE) -C $(ACME_INFRA)/redis status

redis-cli:
	@$(MAKE) -C $(ACME_INFRA)/redis cli

redis-flush:
	@$(MAKE) -C $(ACME_INFRA)/redis flush

# ============================================================================
# Artemis (delegates to acme-infra/artemis)
# ============================================================================

artemis-start:
	@$(MAKE) -C $(ACME_INFRA)/artemis start

artemis-stop:
	@$(MAKE) -C $(ACME_INFRA)/artemis stop

artemis-logs:
	@$(MAKE) -C $(ACME_INFRA)/artemis logs

artemis-status:
	@$(MAKE) -C $(ACME_INFRA)/artemis status
