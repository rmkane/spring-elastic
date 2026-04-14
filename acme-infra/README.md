# ACME infrastructure (local containers)

This directory is **not** a Maven submodule. It holds **Docker Compose** and small **Makefiles** so any project can start shared dev dependencies the same way—clone once, run from here or via `make -C /path/to/acme-infra/...`.

**Demo:** `acme-dev-env.sh` and the defaults here are **versioned on purpose** (localhost, `changeme`, etc.). They are not production secrets—wire real deployments with your own environment and secret management later.

## Layout

| Path | Purpose |
| ---- | ------- |
| `docker-compose.yml` | Elasticsearch, Redis, and optional Artemis (Compose **profile** `artemis`) |
| `elastic/Makefile` | Start/stop ES; CA → `$HOME/.ssh/elastic-ca.crt` |
| `redis/Makefile` | Start/stop Redis, `redis-cli`, flush |
| `artemis/Makefile` | Start/stop Artemis with `--profile artemis` |
| `acme-dev-env.sh` | **Source** in your shell to export ES / Redis / Artemis variables (see below) |

Fixed **container names** (`acme-elasticsearch`, `acme-redis`, `acme-artemis`) and network **`acme-dev-network`** keep behavior predictable when several apps point at `localhost`.

## Quick start

From **this directory**:

```bash
make -C elastic setup    # Elasticsearch + CA to ~/.ssh/elastic-ca.crt
make -C redis start      # Redis on :6379
make -C artemis start    # AMQ broker :61616, console :8161 (optional)
```

From **another repo** that vendors or symlinks `acme-infra`:

```bash
make -C ../acme-infra/elastic setup
```

## Shell environment (`acme-dev-env.sh`)

After containers are up, **source** this file so your shell has the usual dev variables. It is only **`export` lines** (defaults match `docker-compose.yml`). Use **bash** or **zsh** (`source` or `.`).

```bash
source /path/to/acme-infra/acme-dev-env.sh
```

`make -C elastic get-cert` (or `setup`) writes the Elasticsearch CA PEM to **`$HOME/.ssh/elastic-ca.crt`**, matching **`acme-dev-env.sh`** (`ES_CERT_PATH`). **`ES_PASSWORD`** in that script defaults to **`changeme`** (same as Compose unless you set **`ELASTIC_PASSWORD`** in **`acme-infra/.env`**); override with **`export ES_PASSWORD=...`** after sourcing if needed.

Variables in the file (defaults shown):

| Variable | Role |
| -------- | ---- |
| `ES_URIS` | Elasticsearch HTTPS URL (default `https://localhost:9200`) |
| `ES_USERNAME` | ES user (default `elastic`) |
| `ES_PASSWORD` / `ELASTIC_PASSWORD` | Default `changeme` (match Compose); override if you reset the elastic user |
| `ES_CERT_PATH` | `$HOME/.ssh/elastic-ca.crt` — written by `make -C elastic get-cert` / `setup` |
| `ES_CONNECTION_TIMEOUT` / `ES_SOCKET_TIMEOUT` | Elasticsearch client timeouts (defaults `10s` / `60s`) |
| `ELASTIC_API_BASE_URL` | Upstream API for **acme-elastic-consumer** (default `http://localhost:8885`) |
| `REDIS_HOST` | Default `localhost` |
| `REDIS_PORT` | Default `6379` |
| `REDIS_TIMEOUT` | Redis command timeout (default `2s`) |
| `ACME_ARTEMIS_BROKER_URL` | Core protocol URL (default `tcp://localhost:61616`) |
| `ACME_ARTEMIS_USER` / `ACME_ARTEMIS_PASSWORD` | JMS credentials (defaults `artemis` / `artemis`) |
| `ACME_ARTEMIS_ENABLED` | Default `true` |
| `ACME_ARTEMIS_CONCURRENCY` | Default `1-5` |
| `ACME_ARTEMIS_SESSION_CACHE_SIZE` | Default `10` |
| `ACME_JMS_DEMO_QUEUE` | Demo queue name for **acme-artemis-consumer** |
| `ARTEMIS_USER` / `ARTEMIS_PASSWORD` | Same as above, for Compose `.env` |
| `ARTEMIS_WEB_CONSOLE` | Default `http://localhost:8161` (convenience only) |

Spring `application.yml` files map these names explicitly (e.g. `${ES_URIS:…}`, `${ACME_ARTEMIS_BROKER_URL:…}`) so defaults live in YAML and production values come from the environment.

## Redis

Defaults match Spring `REDIS_HOST` / `REDIS_PORT` when you source `acme-dev-env.sh`.

## Artemis

Broker and web console are optional. Enable the service with the Make targets in `artemis/` (they pass `--profile artemis`). Default login matches the Docker image: user **`artemis`**, password **`artemis`** (override with `ARTEMIS_USER` / `ARTEMIS_PASSWORD` in `.env` — see `.env.example`, or edit / re-export after sourcing `acme-dev-env.sh`).

## Cleaning up

```bash
make -C elastic clean    # tears down ES container for this project; removes ~/.ssh/elastic-ca.crt
```

`elastic clean` stops and removes **only** the Elasticsearch container and deletes **`$HOME/.ssh/elastic-ca.crt`**. Redis and Artemis keep running.

To stop **everything** in this compose project:

```bash
docker compose -f docker-compose.yml --project-directory . down
```

(Run from `acme-infra/`.)
