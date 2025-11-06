#!/bin/bash

# Script to list all Elasticsearch indices
# Usage: ./scripts/list-indices.sh [options]
# Options:
#   -v, --verbose    Show detailed index information
#   -h, --help       Show this help message

set -e

# Load environment variables with defaults
ES_URIS="${ES_URIS:-https://localhost:9200}"
ES_USERNAME="${ES_USERNAME:-elastic}"
ES_PASSWORD="${ES_PASSWORD:-}"
ES_CERT_PATH="${ES_CERT_PATH:-}"

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

echo "Listing Elasticsearch indices..."
echo "Connecting to: $ES_URIS"
echo ""

if [ "$VERBOSE" = true ]; then
    # Show detailed index information
    INDICES_RESPONSE=$(${CURL_CMD} "${ES_URIS}/_cat/indices?v&s=index" 2>&1)
    
    if [ $? -eq 0 ]; then
        echo "$INDICES_RESPONSE"
    else
        echo "Error: Failed to connect to Elasticsearch"
        echo "$INDICES_RESPONSE"
        exit 1
    fi
else
    # Show indices in JSON format
    INDICES_RESPONSE=$(${CURL_CMD} "${ES_URIS}/_cat/indices?format=json&s=index" 2>&1)
    
    if [ $? -eq 0 ]; then
        # Pretty print JSON if jq is available, otherwise just show raw
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

