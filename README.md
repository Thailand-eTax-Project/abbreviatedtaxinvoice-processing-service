# Abbreviated Tax Invoice Processing Service

A Spring Boot microservice that processes Abbreviated Tax Invoice XML documents as part of the Thai e-Tax invoice saga orchestration pipeline.

## Overview

This service consumes `saga.command.abbreviated-tax-invoice` commands from the orchestrator, parses and validates abbreviated tax invoice XML using the [teda](https://github.com/wpanther/etax/tree/main/teda) library, persists processed invoices to PostgreSQL, and publishes replies back to the orchestrator via `saga.reply.abbreviated-tax-invoice`.

### Key Characteristics

- **Abbreviated vs Full Tax Invoice**: Abbreviated tax invoices contain only a seller party (no buyer), as defined in the Thai e-Tax specification
- **Event-Driven**: No REST API; operates purely via Kafka command/reply messaging
- **Saga Participant**: Responds to orchestrator commands and compensation requests
- **Hexagonal Architecture**: Clean separation between domain logic and infrastructure adapters

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                     Saga Orchestration Flow                      │
└─────────────────────────────────────────────────────────────────┘

  Orchestrator                    This Service
  ───────────                     ────────────
      │                                │
      │  saga.command.abbreviated-tax-invoice
      │ ─────────────────────────────────────────>
      │                                │
      │                                │ Parse XML (teda)
      │                                │ Calculate totals
      │                                │ Persist to PostgreSQL
      │                                │
      │  saga.reply.abbreviated-tax-invoice
      │ <─────────────────────────────────────────
      │                                │
      │                    abbreviated.taxinvoice.processed
      │                                │ ──────────> Notification Service
      │                                │
      │  saga.compensation.abbreviated-tax-invoice
      │ ─────────────────────────────────────────>
      │                                │ (delete on rollback)
      │  saga.reply.compensated
      │ <─────────────────────────────────────────
```

## Project Structure (Hexagonal/Ports-and-Adapters)

```
src/main/java/com/wpanther/abbreviatedtaxinvoice/processing/
├── AbbreviatedTaxInvoiceProcessingServiceApplication.java
├── application/              # Application layer
│   ├── dto/event/            # Event DTOs
│   ├── port/
│   │   ├── in/               # Input ports (use cases)
│   │   └── out/              # Output ports (interfaces)
│   └── service/              # Application service (use case implementations)
├── domain/                   # Domain layer (framework-independent)
│   ├── event/                # Domain events
│   ├── model/                # Domain model (aggregate root, value objects)
│   └── port/out/             # Domain output port interfaces
└── infrastructure/           # Infrastructure layer (adapters)
    ├── adapter/
    │   ├── in/
    │   │   └── messaging/    # Camel Kafka consumers
    │   └── out/
    │       ├── messaging/    # Kafka event publishers
    │       ├── parsing/      # XML parser adapter (teda)
    │       └── persistence/  # JPA repositories
    ├── config/               # Configuration classes
    └── persistence/
        └── outbox/           # Transactional outbox for可靠 messaging
```

## Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| Runtime | Java | 21 |
| Framework | Spring Boot | 3.2.5 |
| Messaging | Apache Camel + Kafka | 4.14.4 |
| Database | PostgreSQL | 16+ |
| Migrations | Flyway | 10.10.0 |
| XML Parsing | teda (Thai e-Tax Invoice Library) | 1.0.0 |
| Saga Support | saga-commons | 1.0.0-SNAPSHOT |
| Mapping | MapStruct | 1.5.5.Final |
| Build | Maven | 3.6+ |

## Domain Model

### Aggregate Root: `ProcessedAbbreviatedTaxInvoice`

Represents a processed abbreviated tax invoice with:
- **Identity**: `AbbreviatedTaxInvoiceId` (UUID), `sourceInvoiceId`
- **Header**: invoice number, issue date, due date
- **Parties**: Seller only (abbreviated invoices have no buyer)
- **Line Items**: List of `LineItem` (product, quantity, unit price, tax rate)
- **Currency**: 3-letter ISO code
- **Original XML**: Raw XML for audit trail
- **State Machine**: `PENDING → PROCESSING → COMPLETED → PDF_REQUESTED → PDF_GENERATED` (or `FAILED`)

### Value Objects

- **`Money`**: Immutable monetary amount with currency (BigDecimal, 2 decimal places)
- **`Party`**: Seller entity with name, address, tax identifier
- **`LineItem`**: Product line with quantity, unit price, tax rate, line total
- **`Address`**: Thai address with building number, Moo, Soi, Road, Tambon, Amphoe, Province, postal code
- **`TaxIdentifier`**: Tax ID (VAT registration number)

## Kafka Topics

### Consumed Topics

| Topic | Consumer | Purpose |
|-------|----------|---------|
| `saga.command.abbreviated-tax-invoice` | This service | Process abbreviated tax invoice |
| `saga.compensation.abbreviated-tax-invoice` | This service | Rollback processing |

### Produced Topics

| Topic | Producer | Purpose |
|-------|----------|---------|
| `saga.reply.abbreviated-tax-invoice` | This service | Success/failure reply to orchestrator |
| `abbreviated.taxinvoice.processed` | This service | Notify downstream services |

## Configuration

### Environment Variables

| Variable | Default | Description |
|----------|---------|-------------|
| `DB_HOST` | `localhost` | PostgreSQL host |
| `DB_PORT` | `5432` | PostgreSQL port |
| `DB_NAME` | `abbreviatedtaxinvoiceprocess_db` | Database name |
| `DB_USERNAME` | `postgres` | Database user |
| `DB_PASSWORD` | `postgres` | Database password |
| `KAFKA_BROKERS` | `localhost:9092` | Kafka bootstrap servers |
| `EUREKA_URL` | `http://localhost:8761/eureka/` | Eureka service registry |
| `HOSTNAME` | `localhost` | Service hostname |

### Application Properties

Key configurations in `application.yml`:
- **Server port**: 8087
- **Flyway migrations**: `classpath:db/migration`
- **Camel retry**: 3 max redeliveries with exponential backoff
- **Kafka consumers**: 3 consumers per topic, 100 max poll records
- **DLQ**: `abbreviated.taxinvoice.processing.dlq`

## Building and Running

### Prerequisites

1. **PostgreSQL** on `localhost:5432` with database `abbreviatedtaxinvoiceprocess_db`
2. **Kafka** on `localhost:9092`
3. **teda library** installed: `cd ../../teda && mvn clean install`
4. **saga-commons** installed: `cd ../../saga-commons && mvn clean install`

### Build

```bash
# Build the service
mvn clean package

# Skip tests
mvn clean package -DskipTests
```

### Run

```bash
# Run with defaults (requires local PostgreSQL and Kafka)
mvn spring-boot:run

# Run with custom configuration
DB_HOST=db.example.com KAFKA_BROKERS=kafka.example.com:9092 mvn spring-boot:run
```

### Database Migrations

```bash
# Run Flyway migrations
mvn flyway:migrate

# Check migration status
mvn flyway:info
```

## Testing

```bash
# Run all unit tests
mvn clean test

# Run with coverage verification (JaCoCo 80% line coverage per package)
mvn verify

# Run single test class
mvn test -Dtest=ProcessedAbbreviatedTaxInvoiceTest

# Run single test method
mvn test -Dtest=ProcessedAbbreviatedTaxInvoiceTest#testCalculateTotals

# Run integration tests (requires Docker)
mvn verify -Pintegration
```

## Metrics

The service exposes Prometheus metrics via the Actuator endpoint `/actuator/prometheus`.

### Custom Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `abbreviatedtaxinvoice.processing.success` | Counter | Successfully processed invoices |
| `abbreviatedtaxinvoice.processing.failure` | Counter | Failed processing attempts |
| `abbreviatedtaxinvoice.processing.idempotent` | Counter | Duplicate requests handled idempotently |
| `abbreviatedtaxinvoice.processing.race_condition_resolved` | Counter | Concurrent insert race conditions resolved |
| `abbreviatedtaxinvoice.compensation.success` | Counter | Successful compensations |
| `abbreviatedtaxinvoice.compensation.idempotent` | Counter | Duplicate compensations (idempotent) |
| `abbreviatedtaxinvoice.processing.duration` | Timer | Processing time histogram |

## Health Checks

- **Liveness**: `/actuator/health/liveness`
- **Readiness**: `/actuator/health/readiness`
- **Prometheus**: `/actuator/prometheus`

## Database Schema

The service uses Flyway migrations in `src/main/resources/db/migration/`:

| Table | Purpose |
|-------|---------|
| `processed_abbreviated_tax_invoices` | Main entity table |
| `outbox_events` | Transactional outbox for reliable messaging |
| `flyway_schema_history` | Flyway migration tracking |

### Key Constraints

- `uq_processed_abbreviated_tax_invoices_source_invoice_id`: Ensures idempotency by source invoice ID

## Error Handling

### Idempotency

The service handles duplicate processing requests idempotently:
1. On `DuplicateKeyException` for `source_invoice_id`, checks if record exists
2. If existing record is `COMPLETED`, returns success without reprocessing
3. If existing record is `PROCESSING` (mid-flight failure), completes it

### Dead Letter Queue

Failed messages after max redeliveries are sent to:
- `abbreviated.taxinvoice.processing.dlq`

### Compensation

On saga rollback, the orchestrator sends a compensation command:
1. Deletes the processed invoice record
2. Returns `COMPENSATED` reply
3. Handles idempotently if record already absent

## Dependencies

This service depends on:
- **[teda](https://github.com/wpanther/etax/tree/main/teda)**: Thai e-Tax Invoice library for XML validation and JAXB parsing
- **[saga-commons](https://github.com/wpanther/etax/tree/main/saga-commons)**: Saga orchestration patterns and outbox support
- **[eidasremotesigning](https://github.com/wpanther/etax/tree/main/eidasremotesigning)**: Not directly; downstream PDF signing uses this

## Related Services

| Service | Port | Relationship |
|---------|------|--------------|
| orchestrator-service | 8100 | Sends commands, receives replies |
| notification-service | 8099 | Receives `abbreviated.taxinvoice.processed` events |
| xml-signing-service | 8088 | Consumes processed events (future) |
| pdf-signing-service | 8089 | Consumes processed events (future) |