#!/usr/bin/env bash

# Script to check Elasticsearch cluster health
# Usage: ./scripts/check-health.sh
#
# Requires exported env (e.g. source acme-infra/acme-dev-env.sh): ES_URIS, ES_USERNAME,
# ES_PASSWORD, ES_CERT_PATH. This script does not set defaults or read password files.

set -e

require_env() {
    local _n="$1"
    if [ -z "${!_n:-}" ]; then
        echo "Error: ${_n} is not set. Export it first (e.g. source acme-infra/acme-dev-env.sh)." >&2
        exit 1
    fi
}

require_env ES_URIS
require_env ES_USERNAME
require_env ES_PASSWORD
require_env ES_CERT_PATH

# Build curl command
CURL_CMD="curl -s -u ${ES_USERNAME}:${ES_PASSWORD}"

if [ -f "$ES_CERT_PATH" ]; then
    CURL_CMD="${CURL_CMD} --cacert ${ES_CERT_PATH}"
else
    echo "Warning: ES_CERT_PATH file not found (${ES_CERT_PATH}); using curl -k" >&2
    CURL_CMD="${CURL_CMD} -k"
fi

echo "Checking Elasticsearch cluster health..."
echo "Connecting to: $ES_URIS"
echo ""

# Check cluster health
HEALTH_RESPONSE=$(${CURL_CMD} "${ES_URIS}/_cluster/health?pretty" 2>&1)

if [ $? -eq 0 ]; then
    echo "$HEALTH_RESPONSE"
else
    echo "Error: Failed to connect to Elasticsearch"
    echo "$HEALTH_RESPONSE"
    exit 1
fi
