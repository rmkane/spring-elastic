#!/bin/bash

# Script to check Elasticsearch cluster health
# Usage: ./scripts/check-health.sh

set -e

# Load environment variables with defaults
ES_URIS="${ES_URIS:-https://localhost:9200}"
ES_USERNAME="${ES_USERNAME:-elastic}"
ES_PASSWORD="${ES_PASSWORD:-}"
ES_CERT_PATH="${ES_CERT_PATH:-}"

if [ -z "$ES_PASSWORD" ]; then
    if [ -f ".es_password" ]; then
        ES_PASSWORD=$(cat .es_password)
    else
        echo "Error: ES_PASSWORD not set and .es_password file not found"
        echo "Set ES_PASSWORD environment variable or run 'make es-setup'"
        exit 1
    fi
fi

# Build curl command
CURL_CMD="curl -s -u ${ES_USERNAME}:${ES_PASSWORD}"

# Add certificate if provided
if [ -n "$ES_CERT_PATH" ] && [ -f "$ES_CERT_PATH" ]; then
    CURL_CMD="${CURL_CMD} --cacert ${ES_CERT_PATH}"
else
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

