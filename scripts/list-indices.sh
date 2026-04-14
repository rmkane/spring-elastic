#!/usr/bin/env bash

# Script to list all Elasticsearch indices
# Usage: ./scripts/list-indices.sh [options]
# Options:
#   -v, --verbose    Show detailed index information
#   -h, --help       Show this help message
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

VERBOSE=false

# Parse arguments
while [[ $# -gt 0 ]]; do
    case $1 in
        -v|--verbose)
            VERBOSE=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 [options]"
            echo "Options:"
            echo "  -v, --verbose    Show detailed index information"
            echo "  -h, --help       Show this help message"
            exit 0
            ;;
        *)
            echo "Unknown option: $1"
            echo "Use -h or --help for usage information"
            exit 1
            ;;
    esac
done

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

echo "Listing Elasticsearch indices..."
echo "Connecting to: $ES_URIS"
echo ""

if [ "$VERBOSE" = true ]; then
    INDICES_RESPONSE=$(${CURL_CMD} "${ES_URIS}/_cat/indices?v&s=index" 2>&1)

    if [ $? -eq 0 ]; then
        echo "$INDICES_RESPONSE"
    else
        echo "Error: Failed to connect to Elasticsearch"
        echo "$INDICES_RESPONSE"
        exit 1
    fi
else
    INDICES_RESPONSE=$(${CURL_CMD} "${ES_URIS}/_cat/indices?format=json&s=index" 2>&1)

    if [ $? -eq 0 ]; then
        if command -v jq &> /dev/null; then
            echo "$INDICES_RESPONSE" | jq '.'
        else
            echo "$INDICES_RESPONSE"
        fi
    else
        echo "Error: Failed to connect to Elasticsearch"
        echo "$INDICES_RESPONSE"
        exit 1
    fi
fi
