# Convenience entrypoint — infra is not part of Maven; use per-service Makefiles.
.DEFAULT_GOAL := help

INFRA_DIR := $(abspath $(dir $(lastword $(MAKEFILE_LIST))))

.PHONY: help elastic-help redis-help artemis-help

help:
	@echo "ACME shared containers ($(INFRA_DIR))"
	@echo ""
	@echo "Per-service (preferred):"
	@echo "  make -C elastic help    # Elasticsearch"
	@echo "  make -C redis help      # Redis"
	@echo "  make -C artemis help    # Artemis (AMQ)"
	@echo ""
	@echo "Typical flow:"
	@echo "  make -C elastic setup"
	@echo "  make -C redis start"
	@echo "  make -C artemis start   # optional"

elastic-help:
	@$(MAKE) -C $(INFRA_DIR)/elastic help

redis-help:
	@$(MAKE) -C $(INFRA_DIR)/redis help

artemis-help:
	@$(MAKE) -C $(INFRA_DIR)/artemis help
