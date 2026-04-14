# Spring Elastic Application

Multi-module Maven project: **domain** (Elasticsearch models, repositories, services), **API** (REST on port **8885**, talks to Elasticsearch), and **consumer** (REST on port **8883**, HTTP client to the API). Uploads use a **weekly index** name. The API includes **OpenAPI (Swagger UI)** and **Logback** (under `logs/` in the API module).

Shared **Docker Compose** and per-service **Makefiles** live under **`acme-infra/`**, which is **not** a Maven module—use it from this repo or copy/symlink it so several projects can start the same Elasticsearch, Redis, and optional Artemis containers. **`acme-infra/acme-dev-env.sh`** is a **demo** convenience (committed defaults for local Docker); production should use your own env and credentials.

## Features

- **REST API** — upload, list, get by id, batch get, search by file name and/or content type, purge current week’s index
- **Weekly index** — index name is the Monday of the current week in `yyyyMMdd` form (e.g. `20260407`)
- **TLS** — optional CA via `ES_CERT_PATH`; otherwise the client trusts all certs in dev (see `ElasticsearchConfig`)
- **Spring Data Elasticsearch** — `DocumentModel` uses `Instant` for `uploadedAt`, keyword subfields for search
- **Springdoc OpenAPI** — interactive API docs in the browser
- **Utilities** — `StringHelper`, `ContentTypeHelper` (unit-tested)

## Prerequisites

- **Java 21** (see `pom.xml`)
- **Maven 3.9+**
- **Docker** and **Docker Compose** (for local Elasticsearch, Redis, optional Artemis)
- **Make** (optional; wrappers in the repo root delegate to `acme-infra/*/Makefile`)

## Quick start

### 1. Start Elasticsearch (and optionally Redis / Artemis)

From the **repository root** (thin wrappers):

```bash
make es-setup
```

That runs **`make -C acme-infra/elastic setup`**: starts Elasticsearch only, waits, then writes the CA to **`$HOME/.ssh/elastic-ca.crt`**. Use **`acme-dev-env.sh`** for **`ES_PASSWORD`** (default **`changeme`**, or match **`ELASTIC_PASSWORD`** in **`acme-infra/.env`**).

From **any path** (e.g. another project that shares the same `acme-infra` checkout):

```bash
make -C /path/to/acme-infra/elastic setup
```

For **Redis** (consumer cache): `make redis-start` or `make -C acme-infra/redis start`. For **Artemis**: `make artemis-start` or `make -C acme-infra/artemis start`.

Compose file: **`acme-infra/docker-compose.yml`**. Default Elasticsearch password is **`changeme`** unless you set **`ELASTIC_PASSWORD`** in **`acme-infra/.env`** (see **`acme-infra/.env.example`**). More detail: **`acme-infra/README.md`**.

### 2. Environment variables

Easiest: **`source acme-infra/acme-dev-env.sh`** (see **`acme-infra/README.md`**). That sets **`ES_CERT_PATH`** to **`$HOME/.ssh/elastic-ca.crt`**, which **`make es-setup`** / **`make es-get-cert`** populate.

Or set variables yourself before running the app:

```bash
export ES_URIS=https://localhost:9200
export ES_USERNAME=elastic
export ES_PASSWORD=changeme   # or whatever you set as ELASTIC_PASSWORD for Compose
export ES_CERT_PATH="$HOME/.ssh/elastic-ca.crt"
```

If **`ES_CERT_PATH`** is missing or the file does not exist, the app uses a **trust-all** SSL context for development only.

### 3. Run the Elasticsearch API

```bash
mvn -pl acme-elastic-api spring-boot:run
# or
make run-dev
```

API HTTP port: **8885**.

### 4. (Optional) Run the consumer gateway

In another terminal (API must be up on **8885**):

```bash
mvn -pl acme-elastic-consumer spring-boot:run
# or
make run-consumer
```

Consumer port: **8883**. It exposes the same `/api/...` paths and forwards to the API. Override the upstream base URL with **`elastic-api.base-url`** (see `acme-elastic-consumer/src/main/resources/application.yml`).

### 5. OpenAPI (Swagger UI)

With the **API** running:

- [http://localhost:8885/swagger-ui.html](http://localhost:8885/swagger-ui.html)

Use this to try multipart upload and the GET endpoints.

### 6. Upload a file

Run **`curl` from the repository root** so paths like `data/...` resolve, or use absolute paths.

```bash
curl -X POST http://localhost:8885/api/documents/upload \
  -F "file=@data/sample1.txt"
```

Via the consumer (port **8883**):

```bash
curl -X POST http://localhost:8883/api/documents/upload \
  -F "file=@data/sample1.txt"
```

Sample files live under **`data/`** (`sample.json`, `sample2.txt`, etc.). See **`data/README.md`**.

**Success:** `201 Created` with JSON including `id`, `fileName`, `content`, `fileSize`, `contentType`, `uploadedAt` (ISO-8601 instant).

## API summary

| Method | Path | Description |
| ------ | ---- | ----------- |
| `GET` | `/api/documents` | List documents in the **current week’s** index (empty list if index missing) |
| `GET` | `/api/documents/{id}` | Get one document by id (`404` if missing) |
| `GET` | `/api/documents/by-ids?ids=id1,id2` | Batch get (unknown ids omitted) |
| `GET` | `/api/documents/search` | Query params: `fileName` and/or `contentType` (substring match on **`.keyword`** fields) |
| `DELETE` | `/api/documents/index` | Delete the **current week’s** index only |
| `POST` | `/api/documents/upload` | Multipart field **`file`** |

Search requires at least one of **`fileName`** or **`contentType`**. Purge is destructive and unprotected — fine for local dev only.

## Index naming

The index name is **not** `documents-…`. It is computed from the **Monday of the current week** in **`yyyyMMdd`** format, for example:

- Week starting Monday 2026-04-06 → index **`20260406`**

Older weeks remain as separate indices until you remove them in Elasticsearch. **`GET /api/documents`** only reads the **current** week’s index.

After changing mappings (e.g. adding keyword fields), **purge the current index** or wait for a new week’s index, then re-upload.

## Makefile commands

### Application

- `make run-dev` / `make run-api` — Elasticsearch API (port **8885**) with DevTools
- `make run-consumer` — consumer gateway (port **8883**)
- `make test` — `mvn test` (all modules)
- `make build`, `make clean`, etc. — see `make help`

### Shared containers (`acme-infra/`)

- `make infra-help` — points to per-service Makefiles
- **Elasticsearch:** `make es-start` / `make es-stop`, `make es-setup`, `make es-get-cert`, `make es-logs`, `make es-status`, `make es-clean` (delegates to `acme-infra/elastic/`; CA file is **`~/.ssh/elastic-ca.crt`**)
- **Redis:** `make redis-start` … `make redis-flush` (delegates to `acme-infra/redis/`)
- **Artemis:** `make artemis-start` … `make artemis-status` (delegates to `acme-infra/artemis/`)

## Scripts

Shell helpers (run from repo root after **`source acme-infra/acme-dev-env.sh`** — they require **`ES_URIS`**, **`ES_USERNAME`**, **`ES_PASSWORD`**, and **`ES_CERT_PATH`** to already be exported; they do not invent defaults. If the PEM file is missing, they warn and use **`curl -k`**):

- **`scripts/list-indices.sh`** — list indices (`-v` for table view)
- **`scripts/check-index.sh <index-name>`** — index info, optional `--stats`, `--mapping`, `--all`
- **`scripts/check-health.sh`** — cluster health

Example (index name matches your current week):

```bash
./scripts/list-indices.sh
./scripts/check-index.sh 20260406 --all
```

Use the index name that matches **your** current week (see **Index naming** above), or read it from `list-indices.sh` output.

## Configuration

| Variable | Purpose | Default |
| -------- | ------- | ------- |
| `ES_URIS` | Elasticsearch URLs | `https://localhost:9200` |
| `ES_USERNAME` | Basic auth user | `elastic` |
| `ES_PASSWORD` | Basic auth password | (empty in `application.yml`; required in practice) |
| `ES_CERT_PATH` | PEM CA for TLS verify | `acme-dev-env.sh` uses `$HOME/.ssh/elastic-ca.crt` (written by `make es-get-cert` / `es-setup`); missing file → trust-all in dev |
| `LOG_PATH` | Logback file directory | `logs` |
| `LOG_FILE_NAME` | Log file base name | `spring-elastic` |
| `LOG_APP_LEVEL` | Log level for `org.acme.elastic` | `INFO` |

API config: **`acme-elastic-api/src/main/resources/application.yml`**. Consumer config: **`acme-elastic-consumer/src/main/resources/application.yml`**. API logging: **`acme-elastic-api/src/main/resources/logback-spring.xml`** (console + daily rollover under **`logs/`**; that directory is gitignored).

## Tests

```bash
mvn test
```

Includes tests for **`StringHelper`** and **`ContentTypeHelper`**.

## Project structure

```none
spring-elastic/
├── pom.xml                         # parent (packaging pom; modules only — no acme-infra)
├── acme-elastic-domain/            # models, repositories, ES config, services, util, tests
├── acme-elastic-api/              # Spring Boot :8885 — REST controllers, OpenAPI, logback
├── acme-elastic-consumer/         # Spring Boot :8883 — RestClient gateway to API
├── acme-infra/                     # Docker Compose + Makefiles (ES, Redis, Artemis) — not Maven
├── data/                           # sample files for manual upload tests
├── logs/                           # API runtime logs (gitignored)
├── scripts/                        # curl helpers for Elasticsearch
├── Makefile                        # app + delegates to acme-infra/*
└── README.md
```
