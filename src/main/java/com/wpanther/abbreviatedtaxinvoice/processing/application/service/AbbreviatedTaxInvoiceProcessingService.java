package com.wpanther.abbreviatedtaxinvoice.processing.application.service;

import com.wpanther.abbreviatedtaxinvoice.processing.application.dto.event.AbbreviatedTaxInvoiceProcessedEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.AbbreviatedTaxInvoiceId;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessedAbbreviatedTaxInvoice;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessingStatus;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.repository.ProcessedAbbreviatedTaxInvoiceRepository;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.service.AbbreviatedTaxInvoiceParserService;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

/**
 * Application service for abbreviated tax invoice processing.
 * Processes abbreviated tax invoices as part of the saga orchestrator pattern.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class AbbreviatedTaxInvoiceProcessingService {

    private final ProcessedAbbreviatedTaxInvoiceRepository invoiceRepository;
    private final AbbreviatedTaxInvoiceParserService parserService;
    private final EventPublisher eventPublisher;

    /**
     * Process abbreviated tax invoice as part of a saga command.
     * Parses XML, validates, calculates totals, saves to DB, publishes notification event.
     * Does NOT publish xml.signing.requested (orchestrator handles next step).
     *
     * @throws AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException on parse failure
     */
    @Transactional
    public ProcessedAbbreviatedTaxInvoice processInvoiceForSaga(String documentId, String xmlContent,
                                                                 String correlationId)
            throws AbbreviatedTaxInvoiceParserService.AbbreviatedTaxInvoiceParsingException {
        log.info("Processing abbreviated tax invoice for saga, document: {}", documentId);

        // Idempotency check
        Optional<ProcessedAbbreviatedTaxInvoice> existing = invoiceRepository.findBySourceInvoiceId(documentId);
        if (existing.isPresent()) {
            log.warn("Abbreviated tax invoice already processed for document {}, returning existing", documentId);
            return existing.get();
        }

        // Parse XML to domain model
        ProcessedAbbreviatedTaxInvoice invoice = parserService.parseInvoice(xmlContent, documentId);

        // State: PENDING → PROCESSING
        invoice.startProcessing();
        ProcessedAbbreviatedTaxInvoice saved = invoiceRepository.save(invoice);
        log.info("Saved processed abbreviated tax invoice: {}", saved.getInvoiceNumber());

        // State: PROCESSING → COMPLETED
        saved.markCompleted();
        invoiceRepository.save(saved);

        // Publish notification event (kept for notification-service)
        AbbreviatedTaxInvoiceProcessedEvent processedEvent = new AbbreviatedTaxInvoiceProcessedEvent(
            saved.getId().toString(),
            saved.getInvoiceNumber(),
            saved.getTotal().amount(),
            saved.getCurrency(),
            correlationId
        );
        eventPublisher.publishAbbreviatedTaxInvoiceProcessed(processedEvent);

        log.info("Successfully processed abbreviated tax invoice: {}", saved.getInvoiceNumber());
        return saved;
    }

    @Transactional(readOnly = true)
    public Optional<ProcessedAbbreviatedTaxInvoice> findById(String id) {
        try {
            return invoiceRepository.findById(AbbreviatedTaxInvoiceId.from(id));
        } catch (IllegalArgumentException e) {
            log.warn("Invalid abbreviated tax invoice ID format: {}", id);
            return Optional.empty();
        }
    }

    @Transactional(readOnly = true)
    public List<ProcessedAbbreviatedTaxInvoice> findByStatus(ProcessingStatus status) {
        return invoiceRepository.findByStatus(status);
    }
}
