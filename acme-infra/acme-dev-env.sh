#!/usr/bin/env bash
# Demo / local dev only: plain exports matching acme-infra Docker Compose defaults. Kept in
# git on purpose so a clone works out of the box; production uses your own env/secrets.
#
# Source into your current shell (bash or zsh):
#   source /path/to/acme-infra/acme-dev-env.sh
#
# ES_CERT_PATH is the CA from: make -C acme-infra/elastic get-cert (or make setup).
# Override ES_PASSWORD if it is not changeme.

# Shared Elasticsearch variables
export ES_URIS=https://localhost:9200
export ES_USERNAME=elastic
export ES_PASSWORD=changeme
export ELASTIC_PASSWORD=changeme
export ES_CERT_PATH="${HOME}/.ssh/elastic-ca.crt"
export ES_CONNECTION_TIMEOUT=10s
export ES_SOCKET_TIMEOUT=60s

# Shared Redis variables
export REDIS_HOST=localhost
export REDIS_PORT=6379
export REDIS_TIMEOUT=2s

# Shared Artemis variables
export ARTEMIS_USER=artemis
export ARTEMIS_PASSWORD=artemis
export ARTEMIS_WEB_CONSOLE=http://localhost:8161

# Shared Elastic API variables
export ELASTIC_API_BASE_URL=http://localhost:8885

# Shared Artemis variables (Spring acme.artemis.*)
export ACME_ARTEMIS_BROKER_URL=tcp://localhost:61616
export ACME_ARTEMIS_USER=artemis
export ACME_ARTEMIS_PASSWORD=artemis
export ACME_ARTEMIS_ENABLED=true
export ACME_ARTEMIS_CONCURRENCY=1-5
export ACME_ARTEMIS_SESSION_CACHE_SIZE=10
export ACME_JMS_DEMO_QUEUE=acme.demo.queue
