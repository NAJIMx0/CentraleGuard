<div align="center">

# 🛡️ CentraleGuard

**Resilient supervision platform with AI-driven predictive maintenance for industrial equipment**

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.13-blue)](https://www.python.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-streaming-black)](https://kafka.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-containerized-2496ED)](https://www.docker.com/)
[![Jenkins](https://img.shields.io/badge/Jenkins-CI%2FCD-D24939)](https://www.jenkins.io/)
[![Status](https://img.shields.io/badge/status-core%20pipeline%20complete-brightgreen)]()

</div>

---

## Overview

**CentraleGuard** is a simulated industrial telemetry platform for a manufacturing/production environment (compressors, robotic arms, hydraulic presses). It combines a resilient microservices gateway with an AI-based anomaly detection pipeline to demonstrate predictive maintenance: catching abnormal equipment behavior *before* it escalates into a failure, rather than relying solely on fixed alarm thresholds — all wrapped in a full CI/CD pipeline with automated quality gates and live monitoring.

> Most industrial monitoring today reacts to fixed thresholds — *"alert if temperature > 90°C."* By the time that line is crossed, damage may already be underway. CentraleGuard detects statistical deviation from a machine's own normal operating pattern, across multiple signals at once, before a hard threshold is ever reached.

---

## Architecture

```
┌─────────────────────┐     ┌──────────────────────┐
│  telemetry-service   │     │  plc-command-service  │
│  (simulated sensors) │     │  (~30% failure rate)  │
└──────────┬───────────┘     └───────────┬───────────┘
           │                             │
           └──────────────┬──────────────┘
                           ▼
                  ┌─────────────────┐
                  │   API Gateway    │
                  │ ─ Rate limiter   │  (custom token bucket)
                  │ ─ Circuit breaker│  (Resilience4j + fallback)
                  └────────┬─────────┘
                           ▼
                  ┌─────────────────┐
                  │  Apache Kafka    │  sensor-readings topic
                  └────────┬─────────┘
                           ▼
                  ┌─────────────────┐
                  │   AI Service     │  Python / Isolation Forest
                  └────────┬─────────┘
                           ▼
                  ┌─────────────────┐
                  │   TimescaleDB    │  readings + anomaly flags
                  └─────────────────┘

┌──────────────────────────────────────────────────────────┐
│  CI/CD:  GitHub → Jenkins → SonarQube → Docker → Deploy   │
│  Observability:  Prometheus → Grafana                      │
└──────────────────────────────────────────────────────────┘
```

Supporting infrastructure: **Eureka** (service discovery) and **Spring Cloud Config Server** (centralized configuration) tie all services together. Every service is containerized and orchestrated via **Docker Compose**.

---

## Components

| Component | Stack | Role |
|---|---|---|
| `telemetry-service` | Spring Boot | Simulates equipment sensor readings (temperature, vibration, rotation speed, pressure) with realistic drift and injected anomalies |
| `plc-command-service` | Spring Boot | Simulates equipment command execution with a random failure rate |
| `api-gateway` | Spring Cloud Gateway (WebMVC) | Single entry point; enforces rate limiting and circuit breaking on downstream calls |
| `config-server` | Spring Cloud Config | Centralized configuration for all services |
| `discovery-service` | Netflix Eureka | Service registry |
| `ai-service` | Python, scikit-learn, kafka-python | Consumes readings from Kafka, runs anomaly detection, persists results |
| Kafka | Apache Kafka (KRaft) | Streams sensor data between telemetry-service and ai-service |
| TimescaleDB | PostgreSQL + TimescaleDB | Time-series storage for readings and anomaly flags |
| Jenkins | CI/CD | Automated pipeline: checkout → quality scan → build → deploy |
| SonarQube | Static analysis | Code quality, bugs, and security hotspot scanning |
| Prometheus + Grafana | Observability | Metrics collection and live dashboards |

---

## Resilience Engineering

- **Rate limiting** — hand-implemented token bucket algorithm (per-client), protecting the gateway from traffic floods before requests ever reach downstream services.
- **Circuit breaker** — Resilience4j wraps calls to `plc-command-service`. After a threshold of failures within a sliding window, the circuit opens and an instant fallback response is returned instead of hammering a struggling service — preventing cascading failure. State transitions (closed → open → half-open) are exported as live metrics.

---

## Anomaly Detection

Anomaly detection uses an **Isolation Forest** model (scikit-learn), trained on simulated normal operating data collected directly from the live Kafka stream. Rather than a single fixed threshold per sensor, the model learns the *joint* normal pattern across all four signals — temperature, vibration, rotation speed, pressure — and flags readings that deviate from that learned pattern, even when no individual value crosses a hard alarm line. Training data is filtered to exclude intentionally injected spikes, so the model learns from genuinely normal examples only.

---

## CI/CD Pipeline

Every push to `main` triggers an automated Jenkins pipeline:

1. **Checkout** — pulls the latest code from GitHub
2. **SonarQube Analysis** — static code analysis: bugs, code smells, security hotspots, test coverage
3. **Build & Deploy** — Docker images are built for all services and deployed via Docker Compose

```groovy
pipeline {
    agent any
    tools { maven 'Maven3' }
    environment { SONAR_TOKEN = credentials('sonar-token') }
    stages {
        stage('Checkout') { ... }
        stage('SonarQube Analysis') { ... }
        stage('Wait for SonarQube') { ... }
        stage('Deploy with Docker Compose') { ... }
    }
}
```

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

**🚧 Planned**
- Kubernetes orchestration
- GitOps deployment (ArgoCD)
- Service mesh (Istio)

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

This starts every service, plus Kafka, TimescaleDB, SonarQube, Prometheus, and Grafana.

| Service | URL |
|---|---|
| API Gateway | http://localhost:8997 |
| Eureka Dashboard | http://localhost:8761 |
| Grafana | http://localhost:3000 |
| Prometheus | http://localhost:9090 |
| SonarQube | http://localhost:9000 |

### CI/CD

Jenkins pipeline is defined in the repository's `Jenkinsfile`. Point a Jenkins pipeline job (with Docker socket access) at this repository to enable full automated deployment on push.

---

## Author

**Najim** — Final-year Software Engineering student, ÉMSI Tanger
Oracle Certified Professional (Java SE 17) · OCI Foundations Associate