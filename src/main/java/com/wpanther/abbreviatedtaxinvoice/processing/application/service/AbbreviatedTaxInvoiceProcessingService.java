package com.wpanther.abbreviatedtaxinvoice.processing.application.service;

import com.wpanther.abbreviatedtaxinvoice.processing.application.port.in.CompensateAbbreviatedTaxInvoiceUseCase;
import com.wpanther.abbreviatedtaxinvoice.processing.application.port.in.ProcessAbbreviatedTaxInvoiceUseCase;
import com.wpanther.abbreviatedtaxinvoice.processing.application.port.out.AbbreviatedTaxInvoiceEventPublishingPort;
import com.wpanther.abbreviatedtaxinvoice.processing.application.port.out.SagaReplyPort;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.event.AbbreviatedTaxInvoiceProcessedDomainEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessedAbbreviatedTaxInvoice;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessingStatus;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.port.out.AbbreviatedTaxInvoiceParserPort;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.port.out.ProcessedAbbreviatedTaxInvoiceRepository;
import com.wpanther.saga.domain.enums.SagaStep;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.SQLException;
import java.util.Optional;

@Service
@Slf4j
public class AbbreviatedTaxInvoiceProcessingService
        implements ProcessAbbreviatedTaxInvoiceUseCase, CompensateAbbreviatedTaxInvoiceUseCase {

    private final ProcessedAbbreviatedTaxInvoiceRepository invoiceRepository;
    private final AbbreviatedTaxInvoiceParserPort parserPort;
    private final AbbreviatedTaxInvoiceEventPublishingPort eventPublisher;
    private final SagaReplyPort sagaReplyPort;
    private final MeterRegistry meterRegistry;
    private final TransactionTemplate requiresNewTemplate;

    private final Counter processSuccessCounter;
    private final Counter processFailureCounter;
    private final Counter processIdempotentCounter;
    private final Counter processRaceConditionResolvedCounter;
    private final Counter compensateSuccessCounter;
    private final Counter compensateIdempotentCounter;
    private final Counter compensateFailureCounter;
    private final Timer processingTimer;

    public AbbreviatedTaxInvoiceProcessingService(
            ProcessedAbbreviatedTaxInvoiceRepository invoiceRepository,
            AbbreviatedTaxInvoiceParserPort parserPort,
            AbbreviatedTaxInvoiceEventPublishingPort eventPublisher,
            SagaReplyPort sagaReplyPort,
            MeterRegistry meterRegistry,
            PlatformTransactionManager transactionManager) {
        this.invoiceRepository = invoiceRepository;
        this.parserPort = parserPort;
        this.eventPublisher = eventPublisher;
        this.sagaReplyPort = sagaReplyPort;
        this.meterRegistry = meterRegistry;

        TransactionTemplate template = new TransactionTemplate(transactionManager);
        template.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        this.requiresNewTemplate = template;

        this.processSuccessCounter = Counter.builder("abbreviatedtaxinvoice.processing.success")
            .description("Number of successfully processed abbreviated tax invoices")
            .register(meterRegistry);
        this.processFailureCounter = Counter.builder("abbreviatedtaxinvoice.processing.failure")
            .description("Number of failed abbreviated tax invoice processing attempts")
            .register(meterRegistry);
        this.processIdempotentCounter = Counter.builder("abbreviatedtaxinvoice.processing.idempotent")
            .description("Number of duplicate processing requests handled idempotently")
            .register(meterRegistry);
        this.processRaceConditionResolvedCounter = Counter.builder("abbreviatedtaxinvoice.processing.race_condition_resolved")
            .description("Number of DuplicateKeyExceptions on source_invoice_id resolved as concurrent inserts")
            .register(meterRegistry);
        this.compensateSuccessCounter = Counter.builder("abbreviatedtaxinvoice.compensation.success")
            .description("Number of successful compensations")
            .register(meterRegistry);
        this.compensateIdempotentCounter = Counter.builder("abbreviatedtaxinvoice.compensation.idempotent")
            .description("Number of duplicate compensation commands for an already-absent invoice")
            .register(meterRegistry);
        this.compensateFailureCounter = Counter.builder("abbreviatedtaxinvoice.compensation.failure")
            .description("Number of failed compensation attempts")
            .register(meterRegistry);
        this.processingTimer = Timer.builder("abbreviatedtaxinvoice.processing.duration")
            .description("Time taken to process abbreviated tax invoices")
            .register(meterRegistry);
    }

    @Override
    @Transactional
    public void process(String documentId, String xmlContent,
                        String sagaId, SagaStep sagaStep, String correlationId)
            throws AbbreviatedTaxInvoiceProcessingException {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            processInvoiceInternal(documentId, xmlContent, sagaId, sagaStep, correlationId);
        } catch (AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException e) {
            processFailureCounter.increment();
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId, "Parse error: " + e.toString());
            throw new AbbreviatedTaxInvoiceProcessingException(
                "Failed to parse abbreviated tax invoice: " + e.toString(), e);
        } catch (DuplicateKeyException e) {
            if (!isSourceInvoiceIdViolation(e)) {
                processFailureCounter.increment();
                log.error("Duplicate key violation on non-idempotent constraint for document {}, saga {}: {}",
                    documentId, sagaId, e.toString());
                sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                    "Constraint violation for document " + documentId + ": " + e.toString());
                throw new AbbreviatedTaxInvoiceProcessingException(
                    "Constraint violation for document " + documentId, e);
            }
            log.warn("DuplicateKeyException on source_invoice_id for document {}, saga {} — re-checking for concurrent insert",
                documentId, sagaId);
            requiresNewTemplate.execute(txStatus -> {
                Optional<ProcessedAbbreviatedTaxInvoice> existing =
                    invoiceRepository.findBySourceInvoiceId(documentId);
                if (existing.isPresent()) {
                    log.warn("Race condition resolved: document {} already committed by concurrent thread — replying SUCCESS",
                        documentId);
                    processRaceConditionResolvedCounter.increment();
                    sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);
                } else {
                    log.error("DuplicateKeyException on source_invoice_id for document {} but no record found — replying FAILURE",
                        documentId);
                    processFailureCounter.increment();
                    sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                        "Duplicate key violation for document " + documentId + ": " + e.toString());
                }
                return null;
            });
            throw new AbbreviatedTaxInvoiceProcessingException(
                "Concurrent insert for document: " + documentId, e);
        } catch (DataIntegrityViolationException e) {
            processFailureCounter.increment();
            log.error("Constraint violation (non-duplicate-key) for document {}, saga {}: {}",
                documentId, sagaId, e.toString());
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                "Constraint violation for document " + documentId + ": " + e.toString());
            throw new AbbreviatedTaxInvoiceProcessingException(
                "Constraint violation for document " + documentId, e);
        } catch (Exception e) {
            processFailureCounter.increment();
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                "Processing error for document " + documentId + ": " + e.toString());
            throw new AbbreviatedTaxInvoiceProcessingException(
                "Failed to process abbreviated tax invoice " + documentId + ": " + e.toString(), e);
        } finally {
            sample.stop(processingTimer);
        }
    }

    private void processInvoiceInternal(String documentId, String xmlContent,
                                        String sagaId, SagaStep sagaStep, String correlationId)
            throws AbbreviatedTaxInvoiceParserPort.AbbreviatedTaxInvoiceParsingException {
        log.info("Processing abbreviated tax invoice for saga, document: {}", documentId);

        Optional<ProcessedAbbreviatedTaxInvoice> existing =
            invoiceRepository.findBySourceInvoiceId(documentId);
        if (existing.isPresent()) {
            ProcessedAbbreviatedTaxInvoice existingInvoice = existing.get();

            if (existingInvoice.getStatus() == ProcessingStatus.COMPLETED) {
                log.warn("Abbreviated tax invoice already completed for document {}, returning idempotent success",
                    documentId);
                processIdempotentCounter.increment();
                sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);
                return;
            }

            if (existingInvoice.getStatus() == ProcessingStatus.PROCESSING) {
                log.warn("Abbreviated tax invoice for document {} found in PROCESSING state — "
                    + "previous attempt failed mid-flight; resuming completion", documentId);
                existingInvoice.markCompleted();
                invoiceRepository.save(existingInvoice);
                eventPublisher.publish(AbbreviatedTaxInvoiceProcessedDomainEvent.of(
                    existingInvoice.getSourceInvoiceId(),
                    existingInvoice.getInvoiceNumber(),
                    existingInvoice.getTotal(),
                    sagaId, correlationId));
                sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);
                processSuccessCounter.increment();
                log.info("Resumed and completed abbreviated tax invoice: {}", existingInvoice.getInvoiceNumber());
                return;
            }

            throw new IllegalStateException(
                "Abbreviated tax invoice for document " + documentId
                + " has unexpected persisted status: " + existingInvoice.getStatus());
        }

        ProcessedAbbreviatedTaxInvoice invoice = parserPort.parse(xmlContent, documentId);
        invoice.startProcessing();
        ProcessedAbbreviatedTaxInvoice saved = invoiceRepository.save(invoice);
        log.info("Saved processed abbreviated tax invoice: {}", saved.getInvoiceNumber());

        saved.markCompleted();
        invoiceRepository.save(saved);

        eventPublisher.publish(AbbreviatedTaxInvoiceProcessedDomainEvent.of(
            saved.getSourceInvoiceId(), saved.getInvoiceNumber(), saved.getTotal(),
            sagaId, correlationId));
        sagaReplyPort.publishSuccess(sagaId, sagaStep, correlationId);

        processSuccessCounter.increment();
        log.info("Successfully processed abbreviated tax invoice: {}", saved.getInvoiceNumber());
    }

    @Override
    @Transactional
    public void compensate(String documentId, String sagaId, SagaStep sagaStep, String correlationId) {
        log.info("Compensating abbreviated tax invoice for document: {}", documentId);
        try {
            Optional<ProcessedAbbreviatedTaxInvoice> existing =
                invoiceRepository.findBySourceInvoiceId(documentId);
            if (existing.isPresent()) {
                invoiceRepository.deleteById(existing.get().getId());
                log.info("Deleted abbreviated tax invoice for document: {}", documentId);
            } else {
                compensateIdempotentCounter.increment();
                log.warn("Abbreviated tax invoice not found for compensation of document {} saga {} — "
                    + "treating as idempotent duplicate", documentId, sagaId);
            }
            sagaReplyPort.publishCompensated(sagaId, sagaStep, correlationId);
            compensateSuccessCounter.increment();
        } catch (Exception e) {
            compensateFailureCounter.increment();
            log.error("Failed to compensate abbreviated tax invoice for saga {}: {}", sagaId, e.toString(), e);
            sagaReplyPort.publishFailure(sagaId, sagaStep, correlationId,
                "Compensation failed: " + e.toString());
            throw new AbbreviatedTaxInvoiceCompensationException(
                "Compensation failed for document " + documentId, e);
        }
    }

    private static boolean isSourceInvoiceIdViolation(DuplicateKeyException e) {
        Throwable cause = e.getMostSpecificCause();
        String msg = cause.getMessage();
        if (msg == null || !msg.contains("uq_processed_abbreviated_tax_invoices_source_invoice_id")) {
            return false;
        }
        for (Throwable t = e; t != null; t = t.getCause()) {
            if (t instanceof SQLException sqlEx && "23505".equals(sqlEx.getSQLState())) {
                return true;
            }
        }
        return false;
    }
}
