#!/usr/bin/env bash

# Script to check a specific Elasticsearch index
# Usage: ./scripts/check-index.sh <index-name> [options]
# Options:
#   -s, --stats      Show index statistics
#   -m, --mapping    Show index mapping
#   -a, --all        Show all index information
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

SHOW_STATS=false
SHOW_MAPPING=false
SHOW_ALL=false

INDEX_NAME=""
while [[ $# -gt 0 ]]; do
    case $1 in
        -s|--stats)
            SHOW_STATS=true
            shift
            ;;
        -m|--mapping)
            SHOW_MAPPING=true
            shift
            ;;
        -a|--all)
            SHOW_ALL=true
            shift
            ;;
        -h|--help)
            echo "Usage: $0 <index-name> [options]"
            echo ""
            echo "Arguments:"
            echo "  index-name       Name of the index to check"
            echo ""
            echo "Options:"
            echo "  -s, --stats      Show index statistics"
            echo "  -m, --mapping    Show index mapping"
            echo "  -a, --all        Show all index information"
            echo "  -h, --help       Show this help message"
            echo ""
            echo "Examples:"
            echo "  $0 documents-2024-01-08"
            echo "  $0 documents-2024-01-08 --stats"
            echo "  $0 documents-2024-01-08 --all"
            exit 0
            ;;
        -*)
            echo "Unknown option: $1"
            echo "Use -h or --help for usage information"
            exit 1
            ;;
        *)
            if [ -z "$INDEX_NAME" ]; then
                INDEX_NAME="$1"
            else
                echo "Error: Multiple index names provided"
                exit 1
            fi
            shift
            ;;
    esac
done

if [ -z "$INDEX_NAME" ]; then
    echo "Error: Index name is required"
    echo "Usage: $0 <index-name> [options]"
    echo "Use -h or --help for more information"
    exit 1
fi

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

echo "Checking index: $INDEX_NAME"
echo "Connecting to: $ES_URIS"
echo ""

INDEX_EXISTS=$(${CURL_CMD} -o /dev/null -w "%{http_code}" "${ES_URIS}/${INDEX_NAME}" 2>&1)

if [ "$INDEX_EXISTS" != "200" ]; then
    echo "Error: Index '$INDEX_NAME' not found (HTTP $INDEX_EXISTS)"
    echo ""
    echo "Available indices:"
    ${CURL_CMD} "${ES_URIS}/_cat/indices?h=index" 2>&1 | grep -v "^$" || echo "  (none)"
    exit 1
fi

if [ "$SHOW_ALL" = true ] || ([ "$SHOW_STATS" = false ] && [ "$SHOW_MAPPING" = false ]); then
    echo "=== Index Information ==="
    INDEX_INFO=$(${CURL_CMD} "${ES_URIS}/${INDEX_NAME}?pretty" 2>&1)
    if command -v jq &> /dev/null; then
        echo "$INDEX_INFO" | jq '.'
    else
        echo "$INDEX_INFO"
    fi
    echo ""
fi

if [ "$SHOW_ALL" = true ] || [ "$SHOW_STATS" = true ]; then
    echo "=== Index Statistics ==="
    STATS_INFO=$(${CURL_CMD} "${ES_URIS}/${INDEX_NAME}/_stats?pretty" 2>&1)
    if command -v jq &> /dev/null; then
        echo "$STATS_INFO" | jq '.'
    else
        echo "$STATS_INFO"
    fi
    echo ""
fi

if [ "$SHOW_ALL" = true ] || [ "$SHOW_MAPPING" = true ]; then
    echo "=== Index Mapping ==="
    MAPPING_INFO=$(${CURL_CMD} "${ES_URIS}/${INDEX_NAME}/_mapping?pretty" 2>&1)
    if command -v jq &> /dev/null; then
        echo "$MAPPING_INFO" | jq '.'
    else
        echo "$MAPPING_INFO"
    fi
    echo ""
fi

echo "=== Document Count ==="
DOC_COUNT=$(${CURL_CMD} "${ES_URIS}/${INDEX_NAME}/_count?pretty" 2>&1)
if command -v jq &> /dev/null; then
    echo "$DOC_COUNT" | jq '.'
else
    echo "$DOC_COUNT"
fi
