<div align="center">

# 🛡️ CentraleGuard

**Resilient supervision platform with AI-driven predictive maintenance for industrial equipment**

[![Java](https://img.shields.io/badge/Java-17-orange)](https://openjdk.org/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)](https://spring.io/projects/spring-boot)
[![Python](https://img.shields.io/badge/Python-3.11-blue)](https://www.python.org/)
[![Kafka](https://img.shields.io/badge/Apache%20Kafka-streaming-black)](https://kafka.apache.org/)
[![Status](https://img.shields.io/badge/status-in%20progress-yellow)]()

</div>

---

## Overview

**CentraleGuard** is a simulated industrial telemetry platform for a manufacturing/production environment (compressors, robotic arms, hydraulic presses). It combines a resilient microservices gateway with an AI-based anomaly detection pipeline to demonstrate predictive maintenance: catching abnormal equipment behavior *before* it escalates into a failure, rather than relying solely on fixed alarm thresholds.

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
                  │   AI Service     │  Python / FastAPI
                  │ ─ Isolation      │
                  │   Forest model   │
                  └────────┬─────────┘
                           ▼
                  ┌─────────────────┐
                  │   TimescaleDB    │  readings + anomaly flags
                  └─────────────────┘
```

Supporting infrastructure: **Eureka** (service discovery) and **Spring Cloud Config Server** (centralized configuration) tie all services together.

---

## Components

| Component | Stack | Role |
|---|---|---|
| `telemetry-service` | Spring Boot | Simulates equipment sensor readings (temperature, vibration, rotation speed, pressure) with realistic drift and injected anomalies |
| `plc-command-service` | Spring Boot | Simulates equipment command execution with a random failure rate |
| `api-gateway` | Spring Cloud Gateway (WebMVC) | Single entry point; enforces rate limiting and circuit breaking on downstream calls |
| `config-server` | Spring Cloud Config | Centralized configuration for all services |
| `discovery-service` | Netflix Eureka | Service registry |
| `ai-service` | Python, FastAPI, scikit-learn | Consumes readings from Kafka, runs anomaly detection, persists results |
| Kafka | Apache Kafka (KRaft) | Streams sensor data between telemetry-service and ai-service |
| TimescaleDB | PostgreSQL + TimescaleDB | Time-series storage for readings and anomaly flags |

---

## Resilience Engineering

- **Rate limiting** — hand-implemented token bucket algorithm (per-client), protecting the gateway from traffic floods before requests ever reach downstream services.
- **Circuit breaker** — Resilience4j wraps calls to `plc-command-service`. After a threshold of failures within a sliding window, the circuit opens and an instant fallback response is returned instead of hammering a struggling service — preventing cascading failure.

---

## Anomaly Detection

Anomaly detection uses an **Isolation Forest** model (scikit-learn), trained on simulated normal operating data collected directly from the live Kafka stream. Rather than a single fixed threshold per sensor, the model learns the *joint* normal pattern across all four signals — temperature, vibration, rotation speed, pressure — and flags readings that deviate from that learned pattern, even when no individual value crosses a hard alarm line.

---

## Project Status

**✅ Completed**
- Telemetry and PLC command simulation services
- API gateway with custom rate limiter and circuit breaker
- Service discovery and centralized configuration
- Real-time Kafka streaming pipeline
- AI anomaly detection service with TimescaleDB persistence

**🚧 Planned**
- CI/CD pipeline (Jenkins — test → quality gate → build → deploy)
- Containerization (Docker) and orchestration (Kubernetes)
- Observability stack (Prometheus + Grafana)

---

## Getting Started

### Prerequisites
- Java 17, Maven
- Python 3.11+
- Docker

### Run locally

```bash
# 1. Start infrastructure
docker start kafka
docker start timescaledb

# 2. Start core services (in order)
#    config-server → discovery-service → telemetry-service
#    → plc-command-service → api-gateway

# 3. Start the AI service
cd ai-service
venv\Scripts\activate
python consumer.py
```

The gateway is reachable at `http://localhost:8997`.

---

## Author

**Najim** — Final-year Software Engineering student, ÉMSI Tanger
Oracle Certified Professional (Java SE 17) · OCI Foundations Associate
