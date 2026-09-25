<div align="center">

# 🛡️ CentraleGuard

**Resilient supervision platform with AI-driven predictive maintenance for industrial equipment**

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.13-blue)](https://www.python.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-streaming-black)](https://kafka.apache.org/)
[![Kong](https://img.shields.io/badge/Kong-API%20Gateway-003459)](https://konghq.com/)
[![Redis](https://img.shields.io/badge/Redis-shared%20state-DC382D)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-containerized-2496ED)](https://www.docker.com/)
[![Jenkins](https://img.shields.io/badge/Jenkins-CI%2FCD-D24939)](https://www.jenkins.io/)
[![Status](https://img.shields.io/badge/status-in%20active%20development-yellow)]()

</div>

---

## Overview

**CentraleGuard** is a simulated industrial telemetry platform for a manufacturing/production environment (compressors, robotic arms, hydraulic presses). It combines a resilient microservices gateway with an AI-based anomaly detection pipeline to demonstrate predictive maintenance: catching abnormal equipment behavior *before* it escalates into a failure, rather than relying solely on fixed alarm thresholds — all wrapped in a full CI/CD pipeline with automated quality gates and live monitoring.

> Most industrial monitoring today reacts to fixed thresholds — *"alert if temperature > 90°C."* By the time that line is crossed, damage may already be underway. CentraleGuard detects statistical deviation from a machine's own normal operating pattern, across multiple signals at once, before a hard threshold is ever reached.

The project is built incrementally, phase by phase, as a hands-on way to learn distributed-systems concepts rather than just read about them: service discovery, config centralization, event streaming, API gateways, distributed rate limiting, saga-based transactions, authentication, container orchestration, and GitOps — each introduced only once the layer beneath it is solid, and each tied to a concrete bug or limitation it fixes rather than added for its own sake.

---

## Architecture

```
                        ┌─────────────────┐
                        │       Kong        │  API Gateway (routing)
                        └────────┬─────────┘
                                 │
        ┌────────────────────────┴────────────────────────┐
        ▼                                                   ▼
┌─────────────────────┐                         ┌──────────────────────┐
│  telemetry-service   │                         │  plc-command-service  │
│  (simulated sensors) │                         │  (~30% failure rate)  │
└──────────┬───────────┘                         └───────────┬───────────┘
           │                                                  │
           └──────────────────────┬───────────────────────────┘
                                   ▼
                          ┌─────────────────┐
                          │   API Gateway    │  Spring Cloud Gateway
                          │ ─ Rate limiter   │  (Redis-backed token bucket)
                          │ ─ Circuit breaker│  (Resilience4j + fallback)
                          └────────┬─────────┘
                                   ▼
                          ┌─────────────────┐
                          │   Apache Kafka    │  sensor-readings topic
                          └────────┬─────────┘
                                   ▼
                          ┌─────────────────┐
                          │   AI Service      │  Python / Isolation Forest
                          └────────┬─────────┘
                                   ▼
                          ┌─────────────────┐
                          │   TimescaleDB      │  readings + anomaly flags
                          └─────────────────┘

┌──────────────────────────────────────────────────────────┐
│  CI/CD:  GitHub → Jenkins → SonarQube → Docker → Deploy   │
│  Observability:  Prometheus → Grafana                      │
└──────────────────────────────────────────────────────────┘
```

Supporting infrastructure: **Eureka** (service discovery), **Spring Cloud Config Server** (centralized configuration), **Kong** (API Gateway / routing layer), and **Redis** (shared state for distributed rate limiting) tie all services together. Every service is containerized and orchestrated via **Docker Compose**, with health-checked startup ordering so dependent services don't boot before what they rely on is actually ready.

---

## Components

| Component | Stack | Role |
|---|---|---|
| `telemetry-service` | Spring Boot | Simulates equipment sensor readings (temperature, vibration, rotation speed, pressure) with realistic drift, PT100-style quantization, and periodically injected anomalies |
| `plc-command-service` | Spring Boot | Simulates equipment command execution (`reduce-speed`, `stop`) with a random ~30% failure rate, used to exercise the gateway's circuit breaker |
| `api-gateway` | Spring Cloud Gateway (WebMVC) | Single entry point; enforces rate limiting (Redis-backed) and circuit breaking on downstream calls |
| `config-server` | Spring Cloud Config | Centralized configuration for all services, backed by native file-based profiles |
| `discovery-service` | Netflix Eureka | Service registry — every service registers itself so others can find it by name instead of hardcoded host/port |
| `ai-service` | Python, scikit-learn, kafka-python | Consumes readings from Kafka, runs Isolation Forest anomaly detection, persists results to TimescaleDB |
| Kong | API Gateway (declarative, DB-less) | Front-door routing to telemetry-service and plc-command-service, config-as-code via `kong.yml` |
| Konga | Kong Admin GUI | Visual management of Kong routes/services |
| Redis | In-memory data store | Shared state for distributed rate limiting, with idempotency-key storage planned |
| Kafka | Apache Kafka (KRaft mode) | Streams sensor data between telemetry-service and ai-service without a Zookeeper cluster |
| TimescaleDB | PostgreSQL + TimescaleDB | Time-series storage for readings and anomaly flags |
| Jenkins | CI/CD | Automated pipeline: checkout → quality scan → build → deploy |
| SonarQube | Static analysis | Code quality, bugs, and security hotspot scanning |
| Prometheus + Grafana | Observability | Metrics collection and live dashboards |

---

## Resilience Engineering

- **Rate limiting** — token bucket algorithm, migrating from an in-memory `ConcurrentHashMap` to a Redis-backed shared counter. The original implementation had a real distributed-systems bug: each `api-gateway` replica kept its own independent count, so the effective limit silently multiplied by the number of replicas under horizontal scaling. Moving the counter into Redis fixes this by giving every replica a single shared source of truth.
- **Circuit breaker** — Resilience4j wraps calls to `plc-command-service`. After a threshold of failures within a sliding window, the circuit opens and an instant fallback response is returned instead of hammering a struggling service — preventing cascading failure. State transitions (closed → open → half-open) are exported as live metrics.
- **API Gateway routing (Kong)** — declarative, database-free configuration (`kong.yml`) routes external traffic to the correct backend service, eliminating config drift between what's deployed and what's version-controlled.
- **Healthcheck-gated startup ordering** — every stateful/critical service (config-server, discovery-service, Kafka, TimescaleDB) has a Docker healthcheck with an explicit `start_period`, so dependent services wait for real readiness instead of just "container started," which matters once several heavy services (SonarQube, Grafana, Kong) are competing for resources on the same host during boot.

---

## Anomaly Detection

Anomaly detection uses an **Isolation Forest** model (scikit-learn), trained on simulated normal operating data collected directly from the live Kafka stream. Rather than a single fixed threshold per sensor, the model learns the *joint* normal pattern across all four signals — temperature, vibration, rotation speed, pressure — and flags readings that deviate from that learned pattern, even when no individual value crosses a hard alarm line. Training data is filtered to exclude intentionally injected spikes, so the model learns from genuinely normal examples only, rather than learning that spikes are normal.

The `ai-service` consumer loads the trained model once at startup, listens continuously on the `sensor-readings` Kafka topic, scores each incoming reading, and writes the reading plus its anomaly flag into TimescaleDB for later querying and dashboarding.

---

## CI/CD Pipeline

Every push to `main` triggers an automated Jenkins pipeline:

1. **Checkout** — pulls the latest code from GitHub
2. **SonarQube Analysis** — static code analysis: bugs, code smells, security hotspots, test coverage
3. **Build & Deploy** — Docker images are built for all services and deployed via Docker Compose

Planned additions (Phase 9) bring in a fuller DevSecOps chain: Semgrep, dependency scanning, Hadolint, Trivy, Gitleaks, Syft (SBOM), OWASP ZAP (DAST), and cosign for image signing/attestation.

---

## Observability

Grafana dashboards, fed by Prometheus (application metrics) and TimescaleDB (anomaly history):

- **Request traffic** — throughput per route on the gateway
- **Circuit breaker state** — live closed / open / half-open transitions for `plc-command-service`
- **Anomaly count** — detected anomalies over time, queried directly from TimescaleDB

---

## Project Status

**✅ Completed**
- Telemetry and PLC command simulation services
- API gateway with custom rate limiter and circuit breaker
- Service discovery and centralized configuration
- Real-time Kafka streaming pipeline
- AI anomaly detection service with TimescaleDB persistence
- Full containerization (Docker) of all services
- CI/CD pipeline (Jenkins: checkout → SonarQube → build → deploy)
- Observability stack (Prometheus + Grafana) with 3 live dashboards
- Kong API Gateway in declarative (DB-less) mode, routing telemetry-service and plc-command-service
- Konga admin GUI for Kong
- Redis deployed as a container, with healthcheck-gated startup

**🚧 In Progress**
- Rate limiter migration from in-memory `ConcurrentHashMap` to a Redis-backed distributed counter
- Debugging Docker Compose healthcheck timing under multi-service resource contention (Kafka, discovery-service)

**📋 Planned Roadmap**

| Phase | Item | Why it matters |
|---|---|---|
| 2 | Finish Redis-backed rate limiting | Fixes the multiplied-limit bug under horizontal scaling |
| 3 | New services: `notification-service`, `audit-service` | Adds alerting and audit-trail capabilities as real saga participants |
| 4 | Saga pattern (choreography via Kafka) | Coordinates command execution → audit → notification across services without a central transaction |
| 5 | Idempotency key handling (Redis-backed) on `/api/plc/command` | Prevents duplicate machine commands on network retries — a real industrial safety concern |
| 6 | Keycloak (OAuth2/OIDC) authentication at Kong | Protects write/control routes while leaving read-only telemetry open, mirroring real SCADA/HMI patterns |
| 7 | Kong-native rate limiting / IP restriction plugins | Evaluates battle-tested plugins against the hand-built limiter |
| 8 | Load balancing, horizontal scaling, Kong caching on read routes | Proves the distributed fixes from earlier phases actually hold under real replica counts |
| 9 | DevSecOps pipeline (Semgrep, Trivy, Gitleaks, Syft, ZAP, cosign) | Supply-chain security and signed, attested container images |
| 10 | Kubernetes (`kind`) migration for application services | Container orchestration, with stateful infra (Kafka/DB/Keycloak) deliberately kept external |
| 11 | Istio service mesh (mTLS, canary routing) | Zero-trust service-to-service traffic and safe rollout testing |
| 12 | Terraform for infrastructure-as-code | Version-controlled, reproducible infrastructure |
| 13 | ArgoCD (GitOps) | Git as the single source of truth for what's deployed |

---

## Getting Started

### Prerequisites
- Docker & Docker Compose

### Run locally

```bash
git clone https://github.com/NAJIMx0/CentraleGuard.git
cd CentraleGuard
docker compose up --build -d
```

This starts every service, plus Kafka, Redis, TimescaleDB, Kong, Konga, SonarQube, Prometheus, and Grafana. Startup takes a few minutes on first run — services wait on each other's healthchecks before starting, by design.

| Service | URL |
|---|---|
| API Gateway | http://localhost:8997 |
| Kong Proxy | http://localhost:8000 |
| Konga Admin GUI | http://localhost:1337 |
| Eureka Dashboard | http://localhost:8761 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| SonarQube | http://localhost:9000 |

### CI/CD

Jenkins pipeline is defined in the repository's `Jenkinsfile`. Point a Jenkins pipeline job (with Docker socket access) at this repository to enable full automated deployment on push.

---

## Lessons learned along the way

- Docker healthchecks without a `start_period` start counting failures immediately — under load, from multiple heavy services (SonarQube, Grafana, Kong) booting at once, this produces false "unhealthy" failures for services that are actually still starting normally.
- In-memory state inside a service (like a plain Java `Map` used for rate limiting) silently breaks the moment you run more than one replica of that service — each replica has its own copy, with no way to know about the others. Shared state has to live somewhere external (Redis here).
- Konga's Sails.js-based startup can time out on its default 60-second hook window under `NODE_ENV=production` asset builds — solvable via the `KONGA_HOOK_TIMEOUT` env var rather than assuming the image itself is broken.

---

## Author

**Najim** — Final-year Software Engineering student, ÉMSI Tanger
Oracle Certified Professional (Java SE 17) · OCI Foundations Associate