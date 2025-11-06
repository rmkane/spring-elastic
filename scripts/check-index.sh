#!/bin/bash

# Script to check a specific Elasticsearch index
# Usage: ./scripts/check-index.sh <index-name> [options]
# Options:
#   -s, --stats      Show index statistics
#   -m, --mapping    Show index mapping
#   -a, --all        Show all index information
#   -h, --help       Show this help message

set -e

# Load environment variables with defaults
ES_URIS="${ES_URIS:-https://localhost:9200}"
ES_USERNAME="${ES_USERNAME:-elastic}"
ES_PASSWORD="${ES_PASSWORD:-}"
ES_CERT_PATH="${ES_CERT_PATH:-}"

SHOW_STATS=false
SHOW_MAPPING=false
SHOW_ALL=false

# Parse arguments
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

echo "Checking index: $INDEX_NAME"
echo "Connecting to: $ES_URIS"
echo ""

# Check if index exists
INDEX_EXISTS=$(${CURL_CMD} -o /dev/null -w "%{http_code}" "${ES_URIS}/${INDEX_NAME}" 2>&1)

if [ "$INDEX_EXISTS" != "200" ]; then
    echo "Error: Index '$INDEX_NAME' not found (HTTP $INDEX_EXISTS)"
    echo ""
    echo "Available indices:"
    ${CURL_CMD} "${ES_URIS}/_cat/indices?h=index" 2>&1 | grep -v "^$" || echo "  (none)"
    exit 1
fi

# Show index information
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

# Show index statistics
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

# Show index mapping
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

# Show document count
echo "=== Document Count ==="
DOC_COUNT=$(${CURL_CMD} "${ES_URIS}/${INDEX_NAME}/_count?pretty" 2>&1)
if command -v jq &> /dev/null; then
    echo "$DOC_COUNT" | jq '.'
else
    echo "$DOC_COUNT"
fi

