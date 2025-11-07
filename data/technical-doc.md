Technical Documentation Sample

# Spring Elastic Application

## Overview
This application provides file upload functionality with Elasticsearch integration.

## Features
- REST API for file uploads
- Automatic index creation based on weekly dates
- SSL/TLS support for secure connections
- Docker-based Elasticsearch setup

## API Endpoint
POST /api/documents/upload

Accepts multipart file uploads and stores them in Elasticsearch.

## Configuration
The application uses environment variables for Elasticsearch configuration:
- ES_URIS
- ES_USERNAME
- ES_PASSWORD
- ES_CERT_PATH

