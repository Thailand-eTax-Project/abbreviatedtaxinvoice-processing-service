# Refactor: abbreviatedtaxinvoice-processing-service → Hexagonal + DDD (Full Parity with taxinvoice-processing-service)

**Date:** 2026-04-25
**Approach:** Full rewrite using taxinvoice-processing-service as template

---

## Goal

Refactor `abbreviatedtaxinvoice-processing-service` to match the hexagonal (ports & adapters) DDD architecture of `taxinvoice-processing-service` — same directory layout, same port/adapter boundaries, and full behavioral parity including bug fixes present in the reference service.

---

## Scope

- Full structural realignment to hexagonal layout
- Introduction of all port interfaces (application in/out, domain out)
- Behavioral parity: REQUIRES_NEW fix, race-condition handling, PROCESSING-state resume, Micrometer metrics, OutboxCleanupScheduler, HeaderSerializer, KafkaTopicsProperties
- Switch `sagaStep` from raw `String` to typed `SagaStep` enum
- Split single `AbbreviatedTaxInvoiceProcessedEvent` into pure domain event + Kafka DTO
- Unit tests only (no CDC/Kafka consumer integration tests)
- application.yml alignment with taxinvoice config structure

---

## Directory Structure

### Source (`src/main/java/com/wpanther/abbreviatedtaxinvoice/processing/`)

```
application/
├── dto/
│   └── event/
│       └── AbbreviatedTaxInvoiceProcessedEvent.java          # Kafka DTO (moved from domain/event/, retains Jackson)
├── port/
│   ├── in/
│   │   ├── ProcessAbbreviatedTaxInvoiceUseCase.java           # NEW — primary driving port
│   │   └── CompensateAbbreviatedTaxInvoiceUseCase.java        # NEW — primary driving port
│   └── out/
│       ├── SagaReplyPort.java                                 # NEW — secondary driven port
│       └── AbbreviatedTaxInvoiceEventPublishingPort.java      # NEW — secondary driven port
└── service/
    └── AbbreviatedTaxInvoiceProcessingService.java            # REWRITE — implements both in-ports

domain/
├── event/
│   └── AbbreviatedTaxInvoiceProcessedDomainEvent.java         # NEW — pure domain event (no Kafka/Jackson)
├── model/                                                      # UNCHANGED
│   ├── AbbreviatedTaxInvoiceId.java
│   ├── Address.java
│   ├── LineItem.java
│   ├── Money.java
│   ├── Party.java
│   ├── ProcessedAbbreviatedTaxInvoice.java
│   ├── ProcessingStatus.java
│   └── TaxIdentifier.java
└── port/
    └── out/
        ├── ProcessedAbbreviatedTaxInvoiceRepository.java       # MOVE from domain/repository/
        └── AbbreviatedTaxInvoiceParserPort.java                # MOVE+RENAME from domain/service/

infrastructure/
├── adapter/
│   ├── in/
│   │   └── messaging/
│   │       ├── dto/
│   │       │   ├── ProcessAbbreviatedTaxInvoiceCommand.java    # MOVE from domain/event/ + SagaStep enum
│   │       │   └── CompensateAbbreviatedTaxInvoiceCommand.java # MOVE from domain/event/ + SagaStep enum
│   │       ├── SagaCommandHandler.java                         # MOVE from application/service/ + thin adapter
│   │       └── SagaRouteConfig.java                           # MOVE from infrastructure/config/
│   └── out/
│       ├── messaging/
│       │   ├── dto/
│       │   │   └── AbbreviatedTaxInvoiceReplyEvent.java        # MOVE from domain/event/
│       │   ├── SagaReplyPublisher.java                        # REWRITE — implements SagaReplyPort
│       │   ├── AbbreviatedTaxInvoiceEventPublisher.java       # RENAME from EventPublisher + implements port
│       │   └── HeaderSerializer.java                          # NEW — fail-hard toJson
│       ├── parsing/
│       │   └── AbbreviatedTaxInvoiceParserAdapter.java        # RENAME from AbbreviatedTaxInvoiceParserServiceImpl
│       └── persistence/
│           ├── outbox/
│           │   ├── JpaOutboxEventRepository.java               # MOVE from infrastructure/persistence/outbox/
│           │   ├── OutboxEventEntity.java                      # MOVE
│           │   ├── OutboxCleanupScheduler.java                 # NEW
│           │   └── SpringDataOutboxRepository.java             # MOVE
│           ├── AbbreviatedTaxInvoiceLineItemEntity.java        # MOVE
│           ├── AbbreviatedTaxInvoicePartyEntity.java           # MOVE
│           ├── JpaProcessedAbbreviatedTaxInvoiceRepository.java # MOVE
│           ├── ProcessedAbbreviatedTaxInvoiceEntity.java       # MOVE
│           ├── ProcessedAbbreviatedTaxInvoiceMapper.java       # MOVE
│           └── ProcessedAbbreviatedTaxInvoiceRepositoryImpl.java # MOVE
└── config/
    ├── KafkaTopicsProperties.java                              # NEW — typed @ConfigurationProperties record
    └── OutboxConfig.java                                       # UNCHANGED

AbbreviatedTaxInvoiceProcessingServiceApplication.java          # UNCHANGED
```

### Deleted files (replaced by the above)

- `domain/event/ProcessAbbreviatedTaxInvoiceCommand.java`
- `domain/event/CompensateAbbreviatedTaxInvoiceCommand.java`
- `domain/event/AbbreviatedTaxInvoiceReplyEvent.java`
- `domain/event/AbbreviatedTaxInvoiceProcessedEvent.java`
- `domain/repository/ProcessedAbbreviatedTaxInvoiceRepository.java`
- `domain/service/AbbreviatedTaxInvoiceParserService.java`
- `application/service/SagaCommandHandler.java`
- `infrastructure/config/SagaRouteConfig.java`
- `infrastructure/messaging/EventPublisher.java`
- `infrastructure/messaging/SagaReplyPublisher.java`
- `infrastructure/service/AbbreviatedTaxInvoiceParserServiceImpl.java`

---

## Port Interface Contracts

### `application/port/in/ProcessAbbreviatedTaxInvoiceUseCase`

```java
void process(String documentId, String xmlContent,
             String sagaId, SagaStep sagaStep, String correlationId)
    throws AbbreviatedTaxInvoiceProcessingException;

// Checked exception — signals reply already committed to outbox
class AbbreviatedTaxInvoiceProcessingException extends Exception { ... }
```

### `application/port/in/CompensateAbbreviatedTaxInvoiceUseCase`

```java
void compensate(String documentId, String sagaId, SagaStep sagaStep, String correlationId);

// Unchecked — Spring @Transactional rolls back automatically; propagates to Camel DLC
class AbbreviatedTaxInvoiceCompensationException extends RuntimeException { ... }
```

### `application/port/out/SagaReplyPort`

```java
void publishSuccess(String sagaId, SagaStep sagaStep, String correlationId);     // MANDATORY
void publishFailure(String sagaId, SagaStep sagaStep, String correlationId, String errorMessage); // REQUIRES_NEW
void publishCompensated(String sagaId, SagaStep sagaStep, String correlationId); // MANDATORY
```

### `application/port/out/AbbreviatedTaxInvoiceEventPublishingPort`

```java
void publish(AbbreviatedTaxInvoiceProcessedDomainEvent event); // MANDATORY
```

### `domain/port/out/AbbreviatedTaxInvoiceParserPort`

```java
ProcessedAbbreviatedTaxInvoice parse(String xmlContent, String sourceInvoiceId)
    throws AbbreviatedTaxInvoiceParsingException;

// Exception factory methods:
// forEmpty(), forOversized(int, int), forTimeout(long),
// forInterrupted(), forUnmarshal(Throwable), forUnexpectedRootElement(String)
class AbbreviatedTaxInvoiceParsingException extends Exception { ... }
```

### `domain/port/out/ProcessedAbbreviatedTaxInvoiceRepository`

Same method set as current `domain/repository/` interface — moved path only.

---

## Application Service: `AbbreviatedTaxInvoiceProcessingService`

Implements `ProcessAbbreviatedTaxInvoiceUseCase` + `CompensateAbbreviatedTaxInvoiceUseCase`.

**Constructor dependencies:**
- `ProcessedAbbreviatedTaxInvoiceRepository`
- `AbbreviatedTaxInvoiceParserPort`
- `AbbreviatedTaxInvoiceEventPublishingPort`
- `SagaReplyPort`
- `MeterRegistry`
- `PlatformTransactionManager` (for REQUIRES_NEW race-condition template)

### `process()` logic

1. **Idempotency check** via `findBySourceInvoiceId`:
   - `COMPLETED` → publishSuccess, return (idempotent counter++)
   - `PROCESSING` → resume: markCompleted, save, publish domain event, publishSuccess (resume path)
   - Other status → throw `IllegalStateException` (unexpected persisted state)
2. Parse via `AbbreviatedTaxInvoiceParserPort.parse()` (seller-only, no buyer)
3. `startProcessing()` → save → `markCompleted()` → save
4. Publish `AbbreviatedTaxInvoiceProcessedDomainEvent` via `AbbreviatedTaxInvoiceEventPublishingPort`
5. publishSuccess via `SagaReplyPort`

**Exception handling layers:**

| Exception | Action |
|-----------|--------|
| `AbbreviatedTaxInvoiceParsingException` | publishFailure + throw `AbbreviatedTaxInvoiceProcessingException` |
| `DuplicateKeyException` on `uq_processed_abbreviated_tax_invoices_source_invoice_id` | REQUIRES_NEW re-check: if found → publishSuccess (race resolved); if not → publishFailure |
| `DuplicateKeyException` on other constraint | publishFailure + throw `AbbreviatedTaxInvoiceProcessingException` |
| `DataIntegrityViolationException` | publishFailure + throw `AbbreviatedTaxInvoiceProcessingException` |
| `Exception` (catch-all) | publishFailure + throw `AbbreviatedTaxInvoiceProcessingException` |

### `compensate()` logic

1. `findBySourceInvoiceId` → `deleteById` if present; no-op if absent (idempotent counter++)
2. publishCompensated on success
3. On exception: publishFailure + throw `AbbreviatedTaxInvoiceCompensationException`

### Micrometer Metrics

| Metric | Type | Description |
|--------|------|-------------|
| `abbreviatedtaxinvoice.processing.success` | Counter | Successfully processed |
| `abbreviatedtaxinvoice.processing.failure` | Counter | Failed processing attempts |
| `abbreviatedtaxinvoice.processing.idempotent` | Counter | Duplicate commands handled idempotently |
| `abbreviatedtaxinvoice.processing.race_condition_resolved` | Counter | Concurrent inserts resolved via REQUIRES_NEW |
| `abbreviatedtaxinvoice.compensation.success` | Counter | Successful compensations |
| `abbreviatedtaxinvoice.compensation.idempotent` | Counter | Duplicate compensation commands |
| `abbreviatedtaxinvoice.compensation.failure` | Counter | Failed compensations |
| `abbreviatedtaxinvoice.processing.duration` | Timer | Processing wall-clock time |

---

## Infrastructure Adapters

### Inbound: `SagaCommandHandler` (thin adapter)

- `@Component` (not `@Service`)
- Injects `ProcessAbbreviatedTaxInvoiceUseCase` + `CompensateAbbreviatedTaxInvoiceUseCase` ports
- `handleProcessCommand`: calls `process()`, catches only `AbbreviatedTaxInvoiceProcessingException` (reply committed — return normally), lets all other exceptions propagate to DLC
- `handleCompensation`: calls `compensate()` with no catch — `AbbreviatedTaxInvoiceCompensationException` goes to DLC

### Inbound: `SagaRouteConfig`

- Moves from `infrastructure/config/` to `infrastructure/adapter/in/messaging/`
- Injects `KafkaTopicsProperties` typed record instead of `@Value` topic strings
- Adds `kafkaConsumerParams()` helper to deduplicate Kafka URI parameters
- Retry parameters come from `@Value("${app.camel.retry.*}")`
- Adds `RAW()` wrapper around broker URLs

### Outbound: `SagaReplyPublisher` (implements `SagaReplyPort`)

- `publishFailure` uses `@Transactional(propagation = REQUIRES_NEW)` — **bug fix**
- `publishSuccess` / `publishCompensated` use `MANDATORY`
- Uses `HeaderSerializer.toJson()` instead of inline `toJson()`
- Topic injected via `KafkaTopicsProperties.sagaReplyAbbreviatedTaxInvoice()`

### Outbound: `AbbreviatedTaxInvoiceEventPublisher` (implements `AbbreviatedTaxInvoiceEventPublishingPort`)

- Renamed from `EventPublisher`
- Translates `AbbreviatedTaxInvoiceProcessedDomainEvent` → `AbbreviatedTaxInvoiceProcessedEvent` (Kafka DTO) before outbox write
- Uses `MANDATORY` propagation
- Uses `HeaderSerializer.toJson()`

### Outbound: `HeaderSerializer`

- `toJson(Map<String, String>)` throws `IllegalStateException` on failure — **bug fix** (current code returns null silently)

### Outbound: `AbbreviatedTaxInvoiceParserAdapter` (implements `AbbreviatedTaxInvoiceParserPort`)

- Renamed from `AbbreviatedTaxInvoiceParserServiceImpl`
- Method renamed `parseInvoice` → `parse`
- Throw sites use `AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException` factory methods

### Outbound: `OutboxCleanupScheduler`

- Identical to taxinvoice version
- Config: `app.outbox.cleanup.retention-days` + `app.outbox.cleanup.cron`

### `KafkaTopicsProperties`

```java
@ConfigurationProperties(prefix = "app.kafka.topics")
public record KafkaTopicsProperties(
    String abbreviatedTaxinvoiceProcessed,
    String dlq,
    String sagaCommandAbbreviatedTaxInvoice,
    String sagaCompensationAbbreviatedTaxInvoice,
    String sagaReplyAbbreviatedTaxInvoice) {}
```

---

## Domain Event Split

| Class | Package | Purpose |
|-------|---------|---------|
| `AbbreviatedTaxInvoiceProcessedDomainEvent` | `domain/event/` | Pure domain event — no Jackson, no Kafka concerns. Used as `AbbreviatedTaxInvoiceEventPublishingPort` parameter |
| `AbbreviatedTaxInvoiceProcessedEvent` | `application/dto/event/` | Kafka DTO — retains all Jackson annotations and fields from current class |

The infrastructure adapter (`AbbreviatedTaxInvoiceEventPublisher`) translates domain event → Kafka DTO before writing to the outbox.

---

## `SagaStep` Migration

`ProcessAbbreviatedTaxInvoiceCommand` and `CompensateAbbreviatedTaxInvoiceCommand`: `sagaStep` field type changes from `String` to `com.wpanther.saga.domain.enums.SagaStep`.

All port interfaces and application service methods use `SagaStep` — not `String`.

---

## `application.yml` Changes

### Additions

```yaml
spring:
  transaction:
    default-timeout: 30

app:
  parsing:
    timeout-seconds: 10
    max-concurrent: 300
  outbox:
    cleanup:
      retention-days: 7
      cron: "0 0 2 * * *"
  camel:
    retry:
      max-redeliveries: 3
      redelivery-delay-ms: 1000
      backoff-multiplier: 2.0
      max-redelivery-delay-ms: 10000
  kafka:
    consumers:
      max-poll-records: 100
      count: 3
```

### Cleanup

- `management.endpoint.health.show-details`: `always` → `when_authorized`
- Remove `org.hibernate.SQL: DEBUG` and `org.hibernate.type.descriptor.sql.BasicBinder: TRACE` from root logging; move to `dev` profile block
- Add `---\nspring.config.activate.on-profile: dev` block at end of file

---

## Test Structure (Unit Tests Only)

### Tests rewritten to target port interfaces

| Test class | Location | Key changes |
|------------|----------|-------------|
| `AbbreviatedTaxInvoiceProcessingServiceTest` | `application/service/` | Mocks `AbbreviatedTaxInvoiceParserPort`, `SagaReplyPort`, `AbbreviatedTaxInvoiceEventPublishingPort`, `PlatformTransactionManager`. Adds cases: PROCESSING-state resume, DuplicateKey race condition, `DataIntegrityViolationException`, metric counter assertions |
| `SagaCommandHandlerTest` | `infrastructure/adapter/in/messaging/` | Injects use case port mocks (not concrete service). Moves from `application/service/` |

### Tests that move (package rename only)

- `domain/model/*Test` — updated package declarations
- `domain/event/*Test` — updated package declarations
- `infrastructure/adapter/out/messaging/EventPublisherTest` → `AbbreviatedTaxInvoiceEventPublisherTest`
- `infrastructure/adapter/out/messaging/SagaReplyPublisherTest` — updated for `REQUIRES_NEW` behaviour
- New: `infrastructure/adapter/out/messaging/HeaderSerializerTest`
- `infrastructure/adapter/out/persistence/*Test` — moved from `infrastructure/persistence/`
- `infrastructure/adapter/out/parsing/AbbreviatedTaxInvoiceParserAdapterTest` — renamed from `AbbreviatedTaxInvoiceParserServiceImplTest`
- `infrastructure/adapter/in/messaging/SagaRouteConfigTest` — moved from `infrastructure/config/`
- New: `infrastructure/adapter/out/persistence/outbox/OutboxCleanupSchedulerTest`

---

## Bug Fixes Included

| Bug | Location | Fix |
|-----|----------|-----|
| `publishFailure` commits inside ROLLBACK_ONLY outer tx | `SagaReplyPublisher` | `MANDATORY` → `REQUIRES_NEW` |
| `toJson()` returns null silently on serialization error | `SagaReplyPublisher`, `EventPublisher` | New `HeaderSerializer` throws `IllegalStateException` |
| No PROCESSING-state resume | `AbbreviatedTaxInvoiceProcessingService` | Resume path added in idempotency check |
| No DuplicateKey race-condition handling | `AbbreviatedTaxInvoiceProcessingService` | REQUIRES_NEW re-check with constraint name guard |
| Saga reply publishing outside use case boundary | `SagaCommandHandler` | Moved inside application service via `SagaReplyPort` |
