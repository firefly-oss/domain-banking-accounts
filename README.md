# domain-banking-accounts

Domain layer microservice for banking account orchestration within the Firefly platform. This service acts as the orchestration layer for account lifecycle operations, including account opening, closure, balance management, spaces, restrictions, and holder management.

Repository: [https://github.com/firefly-oss/domain-banking-accounts](https://github.com/firefly-oss/domain-banking-accounts)

---

## Overview

`domain-banking-accounts` provides a reactive REST API that coordinates complex, multi-step account management workflows. It sits between upstream consumers (experience services) and the `core-banking-accounts` and `core-common-contract-mgmt` core services, applying domain orchestration through the FireflyFramework Saga Engine and CQRS patterns.

### Key Features

- **Account lifecycle management** -- open, query, and close bank accounts with full contract, party, and balance setup.
- **Account holder management** -- add, update, and remove account holders with role assignments.
- **Balance operations** -- query and manage account balances and balance types.
- **Account spaces** -- create and manage sub-accounts (spaces) for segmented funds.
- **Account restrictions** -- apply, update, and remove account restrictions (holds, blocks, limits).
- **Backoffice operations** -- administrative endpoints for account status, parameters, and history.
- **Saga-orchestrated transactions** -- every write operation is executed through the FireflyFramework `SagaEngine` with compensating steps for rollback on failure.
- **CQRS architecture** -- commands dispatched via `CommandBus`, queries via `QueryBus`, with configurable timeouts and caching.
- **Event-driven architecture** -- Kafka-based event publishing for domain events via the FireflyFramework EDA module.
- **Reactive, non-blocking** -- built on Spring WebFlux with virtual threads enabled.
- **SDK generation** -- auto-generated Java client SDK from the OpenAPI specification.

---

## Architecture

### Module Structure

| Module | Purpose |
|--------|---------|
| `domain-banking-accounts-core` | Business logic: services, commands, handlers, saga workflows, and queries. Organized by subdomain (account, balance, holder, restriction, space, status). |
| `domain-banking-accounts-interfaces` | DTOs and API contracts bridging the web layer to core domain logic. |
| `domain-banking-accounts-infra` | Infrastructure: `ClientFactory` beans for external API clients, `@ConfigurationProperties` configuration. |
| `domain-banking-accounts-web` | Deployable Spring Boot application: REST controllers, application config, actuator, and OpenAPI setup. |
| `domain-banking-accounts-sdk` | Auto-generated Java client SDK from the OpenAPI spec (WebClient-based, reactive). |

### Dependency Flow

```
web --> interfaces --> core --> infra
sdk (generated from openapi.yml)
```

### Core Subdomains

| Subdomain | Description |
|-----------|-------------|
| `account` | Account creation, update, closure via `OpenAccountSaga` |
| `balance` | Balance queries and adjustments |
| `holder` | Account holder registration and role management |
| `restriction` | Account restrictions (holds, blocks, debit/credit limits) |
| `space` | Account spaces (sub-accounts) management |
| `status` | Account status transitions and history |

### Technology Stack

| Technology | Purpose |
|------------|---------|
| Java 25 | Language runtime |
| Spring Boot (WebFlux) | Reactive web framework |
| Project Reactor | Reactive streams |
| Virtual Threads | Enabled via `spring.threads.virtual.enabled: true` |
| FireflyFramework Saga Engine | Distributed saga orchestration |
| FireflyFramework CQRS | Command/Query bus pattern |
| FireflyFramework EDA | Event-driven architecture with Kafka |
| core-banking-accounts-sdk | SDK client for the Banking Accounts core service |
| core-common-contract-mgmt-sdk | SDK client for Contract Management core service |
| SpringDoc OpenAPI | API documentation |
| Micrometer + Prometheus | Metrics and monitoring |

---

## Setup

### Prerequisites

- **Java 25** (or later)
- **Apache Maven 3.9+**
- Access to FireflyFramework Maven repository
- Access to `core-banking-accounts-sdk` and `core-common-contract-mgmt-sdk` artifacts
- **Apache Kafka** (for event publishing)

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `SERVER_ADDRESS` | `localhost` | Address the server binds to |
| `SERVER_PORT` | `8080` | HTTP port |

### Application Configuration

Key settings from `application.yaml`:

```yaml
spring:
  application:
    name: domain-banking-accounts
  threads:
    virtual:
      enabled: true

firefly:
  cqrs:
    enabled: true
    command:
      timeout: 30s
      metrics-enabled: true
      tracing-enabled: true
    query:
      timeout: 15s
      caching-enabled: true
      cache-ttl: 15m
  saga.performance.enabled: true
  eda:
    enabled: true
    default-publisher-type: KAFKA
    publishers:
      kafka:
        default:
          default-topic: domain-layer
          bootstrap-servers: localhost:9092

api-configuration:
  core-banking:
    accounts:
      base-path: http://localhost:8081
  core-common:
    contracts:
      base-path: http://localhost:8082
```

### Build

```bash
# Full build (includes SDK generation)
mvn clean install

# Skip tests
mvn clean install -DskipTests
```

### Run

```bash
# Run via Spring Boot Maven plugin
mvn -pl domain-banking-accounts-web spring-boot:run

# Run with a specific profile
mvn -pl domain-banking-accounts-web spring-boot:run -Dspring-boot.run.profiles=dev

# Or run the packaged JAR
java -jar domain-banking-accounts-web/target/domain-banking-accounts.jar
```

---

## API Endpoints

All endpoints return reactive `Mono<ResponseEntity>` or `Flux<ResponseEntity>` responses.

### Accounts (`/api/v1/accounts`)

| Method | Path | Summary |
|--------|------|---------|
| `POST` | `/api/v1/accounts` | Open a new account (saga-orchestrated) |
| `POST` | `/api/v1/accounts/{accountId}/close` | Close an account |

### Account Backoffice (`/api/v1/backoffice/accounts`)

| Method | Path | Summary |
|--------|------|---------|
| `GET` | `/api/v1/backoffice/accounts/{accountId}` | Get account details |
| `GET` | `/api/v1/backoffice/accounts/{accountId}/balance` | Get account balances |
| `GET` | `/api/v1/backoffice/accounts/{accountId}/holders` | Get account holders |
| `GET` | `/api/v1/backoffice/accounts/{accountId}/spaces` | Get account spaces |
| `GET` | `/api/v1/backoffice/accounts/{accountId}/restrictions` | Get active restrictions |
| `GET` | `/api/v1/backoffice/accounts/{accountId}/restrictions/history` | Get restrictions history |
| `GET` | `/api/v1/backoffice/accounts/{accountId}/parameters` | Get account parameters |
| `GET` | `/api/v1/backoffice/accounts/{accountId}/status-history` | Get status history |
| `DELETE` | `/api/v1/backoffice/accounts/{accountId}/holders/{partyId}` | Remove account holder |
| `PUT` | `/api/v1/backoffice/accounts/{accountId}/status` | Force account status |
| `PUT` | `/api/v1/backoffice/accounts/{accountId}/restrictions/{restrictionId}` | Update restriction |
| `DELETE` | `/api/v1/backoffice/accounts/{accountId}/restrictions/{restrictionId}` | Remove restriction |
| `DELETE` | `/api/v1/backoffice/accounts/{accountId}/spaces/{spaceId}` | Delete space |

### Account Holders (`/api/v1/accounts/{accountId}/holders`)

| Method | Path | Summary |
|--------|------|---------|
| `POST` | `/api/v1/accounts/{accountId}/holders` | Add account holder |
| `PUT` | `/api/v1/accounts/{accountId}/holders/{partyId}` | Update holder role |
| `DELETE` | `/api/v1/accounts/{accountId}/holders/{partyId}` | Remove holder |

### Account Restrictions (`/api/v1/accounts/{accountId}/restrictions`)

| Method | Path | Summary |
|--------|------|---------|
| `POST` | `/api/v1/accounts/{accountId}/restrictions` | Apply restriction |
| `DELETE` | `/api/v1/accounts/{accountId}/restrictions/{restrictionId}` | Remove restriction |

### Account Spaces (`/api/v1/accounts/{accountId}/spaces`)

| Method | Path | Summary |
|--------|------|---------|
| `POST` | `/api/v1/accounts/{accountId}/spaces` | Create space |
| `PUT` | `/api/v1/accounts/{accountId}/spaces/{spaceId}` | Update space |
| `DELETE` | `/api/v1/accounts/{accountId}/spaces/{spaceId}` | Delete space |

---

## Monitoring

### Actuator Endpoints

| Endpoint | Purpose |
|----------|---------|
| `/actuator/health` | Application health with liveness and readiness probes |
| `/actuator/info` | Build information |
| `/actuator/prometheus` | Prometheus-compatible metrics |

### API Documentation

- **Swagger UI**: `/swagger-ui.html` (disabled in `prod` profile)
- **OpenAPI JSON**: `/v3/api-docs`

---

## Spring Profiles

| Profile | Logging | Swagger | Notes |
|---------|---------|---------|-------|
| `default` | INFO | Enabled | Standard development |
| `dev` | DEBUG | Enabled | Verbose debugging |
| `prod` | WARN | Disabled | Production |
