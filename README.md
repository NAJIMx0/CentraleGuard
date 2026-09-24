<div align="center">

# 🛡️ CentraleGuard

**A resilient, event-driven microservices platform for industrial equipment supervision and AI-driven predictive maintenance**

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.13-blue)](https://www.python.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-streaming-black)](https://kafka.apache.org/)
[![Kong](https://img.shields.io/badge/Kong-API%20Gateway-003459)](https://konghq.com/)
[![Redis](https://img.shields.io/badge/Redis-shared%20state-DC382D)](https://redis.io/)
[![Docker](https://img.shields.io/badge/Docker-containerized-2496ED)](https://www.docker.com/)
[![Jenkins](https://img.shields.io/badge/Jenkins-CI%2FCD-D24939)](https://www.jenkins.io/)
[![License](https://img.shields.io/badge/status-active%20development-yellow)]()

[Overview](#overview) · [Architecture](#architecture) · [Components](#components) · [Resilience](#resilience-engineering) · [Roadmap](#roadmap) · [Getting Started](#getting-started)

</div>

---

## Overview

**CentraleGuard** is a simulated industrial telemetry platform for a manufacturing environment (compressors, robotic arms, hydraulic presses). It pairs a resilient microservices gateway with an AI-driven anomaly detection pipeline to demonstrate predictive maintenance — identifying abnormal equipment behavior before it escalates into failure, rather than relying solely on fixed alarm thresholds. The system is delivered through a full CI/CD pipeline with automated quality gates and live observability.

> Conventional industrial monitoring reacts to fixed thresholds — *"alert if temperature exceeds 90°C."* By the time that line is crossed, damage may already be underway. CentraleGuard instead detects statistical deviation from a machine's own normal operating pattern across multiple correlated signals, surfacing anomalies before any single hard threshold is breached.

The platform is developed incrementally, one architectural concern at a time: service discovery, centralized configuration, event streaming, API gateway routing, distributed rate limiting, saga-based transaction coordination, authentication, container orchestration, and GitOps. Each layer is introduced once the one beneath it is stable, and each addition is motivated by a concrete limitation it resolves rather than adopted for its own sake.

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

Supporting infrastructure — **Eureka** (service discovery), **Spring Cloud Config Server** (centralized configuration), **Kong** (API gateway and routing layer), and **Redis** (shared state for distributed rate limiting) — ties the platform together. Every service is containerized and orchestrated via **Docker Compose**, with healthcheck-gated startup ordering ensuring dependent services boot only once their dependencies are genuinely ready, not merely running.

---

## Components

| Component | Stack | Responsibility |
|---|---|---|
| `telemetry-service` | Spring Boot | Simulates equipment sensor readings (temperature, vibration, rotation speed, pressure) with realistic drift, sensor-accurate quantization, and periodically injected anomalies |
| `plc-command-service` | Spring Boot | Simulates equipment command execution (`reduce-speed`, `stop`) with a randomized ~30% failure rate, used to validate the gateway's circuit breaker |
| `api-gateway` | Spring Cloud Gateway (WebMVC) | Primary application entry point; enforces Redis-backed distributed rate limiting and circuit breaking on downstream calls |
| `config-server` | Spring Cloud Config | Centralized configuration management across all services |
| `discovery-service` | Netflix Eureka | Service registry enabling name-based service discovery instead of hardcoded endpoints |
| `ai-service` | Python, scikit-learn, kafka-python | Consumes telemetry from Kafka, performs Isolation Forest anomaly detection, and persists results to TimescaleDB |
| Kong | API Gateway (declarative, DB-less) | Edge routing to `telemetry-service` and `plc-command-service`, configured as code via `kong.yml` |
| Konga | Kong Admin GUI | Visual management interface for Kong routes and services |
| Redis | In-memory data store | Shared state for distributed rate limiting, with idempotency-key storage planned |
| Kafka | Apache Kafka (KRaft mode) | Streams sensor telemetry from `telemetry-service` to `ai-service`, without a separate Zookeeper cluster |
| TimescaleDB | PostgreSQL + TimescaleDB | Time-series persistence for sensor readings and anomaly flags |
| Jenkins | CI/CD | Automated pipeline: checkout → static analysis → build → deploy |
| SonarQube | Static analysis | Code quality, bug detection, and security hotspot scanning |
| Prometheus + Grafana | Observability | Metrics collection and real-time dashboards |

---

## Resilience Engineering

- **Distributed rate limiting** — a token bucket algorithm, migrating from an in-memory `ConcurrentHashMap` to a Redis-backed shared counter. The original implementation carried a genuine distributed-systems defect: each `api-gateway` replica maintained an independent count, so the effective limit scaled linearly with replica count under horizontal scaling. Centralizing counter state in Redis gives every replica a single, consistent source of truth.
- **Circuit breaking** — Resilience4j wraps calls to `plc-command-service`. Once failures within a sliding window exceed a configured threshold, the circuit opens and an immediate fallback response is returned rather than continuing to call a degraded service — preventing cascading failure. Closed → open → half-open state transitions are exported as live metrics.
- **Declarative API gateway routing** — Kong's database-free configuration (`kong.yml`) routes external traffic to the correct backend service as version-controlled configuration, eliminating drift between deployed state and source control.
- **Healthcheck-gated startup ordering** — every stateful or critical service (`config-server`, `discovery-service`, Kafka, TimescaleDB) defines a Docker healthcheck with an explicit `start_period`, ensuring dependent services wait for genuine readiness rather than mere container start — a distinction that matters once multiple resource-intensive services (SonarQube, Grafana, Kong) are competing for host resources during boot.

---

## Anomaly Detection

Anomaly detection uses an **Isolation Forest** model (scikit-learn), trained on simulated normal-operation data collected directly from the live Kafka stream. Rather than applying a single fixed threshold per sensor, the model learns the *joint* distribution across all four signals — temperature, vibration, rotation speed, pressure — and flags readings that deviate from that learned pattern, even when no individual value crosses a hard alarm line. Training data is filtered to exclude intentionally injected spikes, ensuring the model learns exclusively from genuinely normal operating conditions.

The `ai-service` consumer loads the trained model once at startup, continuously consumes the `sensor-readings` Kafka topic, scores each incoming reading, and persists the reading together with its anomaly flag to TimescaleDB for downstream querying and dashboarding.

---

## CI/CD Pipeline

Every push to `main` triggers an automated Jenkins pipeline:

1. **Checkout** — pulls the latest revision from GitHub
2. **Static analysis** — SonarQube scans for bugs, code smells, security hotspots, and test coverage
3. **Build & deploy** — Docker images are built for all services and deployed via Docker Compose

A fuller DevSecOps chain is planned (see [Roadmap](#roadmap)), extending the pipeline with Semgrep, dependency scanning, Hadolint, Trivy, Gitleaks, Syft (SBOM generation), OWASP ZAP (DAST), and cosign for image signing and attestation.

---

## Observability

Grafana dashboards, fed by Prometheus (application metrics) and TimescaleDB (anomaly history), surface:

- **Request traffic** — throughput per route at the gateway
- **Circuit breaker state** — live closed / open / half-open transitions for `plc-command-service`
- **Anomaly trends** — detected anomalies over time, queried directly from TimescaleDB

---

## Project Status

### Completed
- Telemetry and PLC command simulation services
- API gateway with custom rate limiter and circuit breaker
- Service discovery and centralized configuration
- Real-time Kafka streaming pipeline
- AI anomaly detection service with TimescaleDB persistence
- Full containerization of all services
- CI/CD pipeline (Jenkins: checkout → SonarQube → build → deploy)
- Observability stack (Prometheus + Grafana) with three live dashboards
- Kong API gateway in declarative (DB-less) mode, routing `telemetry-service` and `plc-command-service`
- Konga administrative GUI for Kong
- Redis deployed as a healthcheck-gated container

### In Progress
- Migration of the rate limiter from in-memory state to a Redis-backed distributed counter
- Stabilization of Docker Compose healthcheck timing under multi-service resource contention

---

## Roadmap

| Phase | Item | Rationale |
|---|---|---|
| 2 | Complete Redis-backed rate limiting | Resolves the multiplied-limit defect under horizontal scaling |
| 3 | Introduce `notification-service` and `audit-service` | Adds alerting and audit-trail capabilities as full saga participants |
| 4 | Saga pattern via Kafka choreography | Coordinates command execution, auditing, and notification without a central transaction coordinator |
| 5 | Redis-backed idempotency keys on `/api/plc/command` | Prevents duplicate machine commands under network retries — a genuine industrial safety concern |
| 6 | Keycloak (OAuth2/OIDC) authentication at Kong | Protects write/control routes while leaving read-only telemetry open, mirroring real SCADA/HMI access patterns |
| 7 | Kong-native rate limiting and IP restriction plugins | Evaluates battle-tested plugins against the hand-built implementation |
| 8 | Load balancing, horizontal scaling, Kong response caching | Validates that earlier distributed-systems fixes hold under real multi-replica load |
| 9 | Full DevSecOps pipeline (Semgrep, Trivy, Gitleaks, Syft, ZAP, cosign) | Supply-chain security with signed, attested container images |
| 10 | Kubernetes (`kind`) migration for application services | Container orchestration, with stateful infrastructure (Kafka, databases, Keycloak) deliberately kept external |
| 11 | Istio service mesh (mTLS, canary routing) | Zero-trust service-to-service communication and safe progressive rollout |
| 12 | Terraform for infrastructure as code | Version-controlled, reproducible infrastructure provisioning |
| 13 | ArgoCD (GitOps) | Git as the single source of truth for deployed state |

---

## Getting Started

### Prerequisites
- Docker and Docker Compose

### Run locally

```bash
git clone https://github.com/NAJIMx0/CentraleGuard.git
cd CentraleGuard
docker compose up --build -d
```

This launches every service — including Kafka, Redis, TimescaleDB, Kong, Konga, SonarQube, Prometheus, and Grafana. Initial startup takes several minutes, as services intentionally wait on upstream healthchecks before initializing.

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

The Jenkins pipeline is defined in the repository's `Jenkinsfile`. Point a Jenkins pipeline job — with Docker socket access — at this repository to enable fully automated build and deployment on every push.

---

## Engineering Notes

A few operational findings worth documenting, as they reflect real constraints of running a multi-service platform locally rather than theoretical concerns:

- **Healthcheck timing under contention.** Docker healthchecks without an explicit `start_period` begin counting failures immediately. Under load — with several resource-intensive services (SonarQube, Grafana, Kong) starting concurrently — this produces false "unhealthy" failures for services that are still initializing normally rather than actually broken.
- **In-memory state does not survive horizontal scaling.** A plain in-process data structure (such as a Java `Map` used for rate limiting) silently breaks once more than one replica of a service is running, since each replica holds an independent, unsynchronized copy. Shared state must live in an external store — Redis, in this platform.
- **Third-party image quirks are usually documented, not exceptional.** Konga's Sails.js-based startup can exceed its default 60-second initialization window during asset compilation under `NODE_ENV=production`; this is a known, documented behavior resolved via the `KONGA_HOOK_TIMEOUT` environment variable rather than an indication of a broken image.

---

## Author

**Najim** — Final-year Software Engineering student, ÉMSI Tanger
Oracle Certified Professional (Java SE 17) · OCI Foundations Associate