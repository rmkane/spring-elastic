# Spring Elastic Application

A Spring Boot application that accepts file uploads and stores them in Elasticsearch with weekly-based index naming.

## Features

- File upload endpoint that accepts multipart files
- Automatic index creation based on the first day of the current week (format: `documents-YYYY-MM-DD`)
- Elasticsearch integration with SSL/TLS support
- Docker-based Elasticsearch setup via Makefile

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Docker and Docker Compose
- Make

## Quick Start

### 1. Start Elasticsearch

```bash
make es-setup
```

This will:

- Start an Elasticsearch Docker container
- Extract the SSL certificate
- Extract the password
- Display the environment variables you need to set

### 2. Set Environment Variables

After running `make es-setup`, set the environment variables:

```bash
export ES_PASSWORD=$(cat .es_password)
export ES_CERT_PATH=$(pwd)/es-certs/ca.crt
export ES_URIS=https://localhost:9200
export ES_USERNAME=elastic
```

### 3. Run the Application

```bash
mvn spring-boot:run
```

### 4. Upload a File

```bash
curl -X POST http://localhost:8080/api/documents/upload \
  -F "file=@/path/to/your/file.txt"
```

## Makefile Commands

- `make es-start` - Start Elasticsearch container
- `make es-stop` - Stop Elasticsearch container
- `make es-logs` - View Elasticsearch logs
- `make es-get-cert` - Extract certificate from container
- `make es-get-password` - Extract password from container
- `make es-setup` - Complete setup (start, get cert, get password)
- `make es-status` - Check container status
- `make es-clean` - Remove container and cleanup files

## Scripts

The `scripts/` directory contains utility scripts for checking Elasticsearch:

### check-health.sh

Check Elasticsearch cluster health.

```bash
./scripts/check-health.sh
```

### list-indices.sh

List all Elasticsearch indices.

```bash
# Simple list
./scripts/list-indices.sh

# Verbose output (table format)
./scripts/list-indices.sh --verbose
```

### check-index.sh

Check a specific index. Shows index information, document count, and optionally statistics and mapping.

```bash
# Basic index information
./scripts/check-index.sh documents-2024-01-08

# Show statistics
./scripts/check-index.sh documents-2024-01-08 --stats

# Show mapping
./scripts/check-index.sh documents-2024-01-08 --mapping

# Show all information
./scripts/check-index.sh documents-2024-01-08 --all
```

**Note:** All scripts automatically use environment variables (`ES_URIS`, `ES_USERNAME`, `ES_PASSWORD`, `ES_CERT_PATH`) or read from `.es_password` and `es-certs/ca.crt` if available.

## API Endpoints

### POST /api/documents/upload

Upload a file to Elasticsearch.

**Request:**

- Method: POST
- Content-Type: multipart/form-data
- Parameter: `file` (MultipartFile)

**Response:**

- 201 Created: Document saved successfully
- 400 Bad Request: Empty file
- 500 Internal Server Error: Processing error

## Configuration

Environment variables:

- `ES_URIS` - Elasticsearch URIs (default: `https://localhost:9200`)
- `ES_USERNAME` - Elasticsearch username (default: `elastic`)
- `ES_PASSWORD` - Elasticsearch password (required)
- `ES_CERT_PATH` - Path to CA certificate file (required for SSL)
- `ES_SSL_VERIFICATION_MODE` - SSL verification mode (default: `full`)

## Index Naming

Documents are stored in indexes named `documents-YYYY-MM-DD` where the date is the Monday of the current week. For example:

- If today is Wednesday, January 10, 2024, the index will be `documents-2024-01-08` (the Monday of that week)

## Project Structure

```none
spring-elastic/
├── scripts/
│   ├── check-health.sh
│   ├── check-index.sh
│   └── list-indices.sh
├── src/
│   ├── main/
│   │   ├── java/com/example/springelastic/
│   │   │   ├── config/
│   │   │   │   ├── ElasticsearchConfig.java
│   │   │   │   └── IndexNameProvider.java
│   │   │   ├── controller/
│   │   │   │   └── DocumentController.java
│   │   │   ├── model/
│   │   │   │   └── DocumentModel.java
│   │   │   ├── repository/
│   │   │   │   └── DocumentRepository.java
│   │   │   ├── service/
│   │   │   │   └── DocumentService.java
│   │   │   └── SpringElasticApplication.java
│   │   └── resources/
│   │       └── application.yml
├── Makefile
├── pom.xml
└── README.md
```
