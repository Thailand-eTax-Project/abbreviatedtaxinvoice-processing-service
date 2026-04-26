package com.wpanther.abbreviatedtaxinvoice.processing.application.service;

import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging.dto.CompensateAbbreviatedTaxInvoiceCommand;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging.dto.ProcessAbbreviatedTaxInvoiceCommand;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessedAbbreviatedTaxInvoice;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.repository.ProcessedAbbreviatedTaxInvoiceRepository;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.messaging.SagaReplyPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

/**
 * Handles saga commands from the orchestrator.
 * Delegates business logic to AbbreviatedTaxInvoiceProcessingService and sends replies.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class SagaCommandHandler {

    private final AbbreviatedTaxInvoiceProcessingService processingService;
    private final SagaReplyPublisher sagaReplyPublisher;
    private final ProcessedAbbreviatedTaxInvoiceRepository invoiceRepository;

    /**
     * Handle a ProcessAbbreviatedTaxInvoiceCommand from the saga orchestrator.
     * Processes the abbreviated tax invoice and sends a SUCCESS or FAILURE reply.
     */
    @Transactional
    public void handleProcessCommand(ProcessAbbreviatedTaxInvoiceCommand command) {
        log.info("Handling ProcessAbbreviatedTaxInvoiceCommand for saga {} document {}",
            command.getSagaId(), command.getDocumentId());

        try {
            processingService.processInvoiceForSaga(
                command.getDocumentId(),
                command.getXmlContent(),
                command.getCorrelationId()
            );

            sagaReplyPublisher.publishSuccess(
                command.getSagaId(),
                command.getSagaStep(),
                command.getCorrelationId()
            );

            log.info("Successfully processed abbreviated tax invoice for saga {}", command.getSagaId());

        } catch (Exception e) {
            log.error("Failed to process abbreviated tax invoice for saga {}: {}",
                command.getSagaId(), e.getMessage(), e);

            sagaReplyPublisher.publishFailure(
                command.getSagaId(),
                command.getSagaStep(),
                command.getCorrelationId(),
                e.getMessage()
            );
        }
    }

    /**
     * Handle a CompensateAbbreviatedTaxInvoiceCommand from the saga orchestrator.
     * Hard deletes the processed abbreviated tax invoice and sends a COMPENSATED reply.
     */
    @Transactional
    public void handleCompensation(CompensateAbbreviatedTaxInvoiceCommand command) {
        log.info("Handling compensation for saga {} document {}",
            command.getSagaId(), command.getDocumentId());

        try {
            Optional<ProcessedAbbreviatedTaxInvoice> existing =
                invoiceRepository.findBySourceInvoiceId(command.getDocumentId());

            if (existing.isPresent()) {
                invoiceRepository.deleteById(existing.get().getId());
                log.info("Deleted ProcessedAbbreviatedTaxInvoice {} for compensation",
                    existing.get().getId());
            } else {
                log.info("No ProcessedAbbreviatedTaxInvoice found for document {} - already compensated or never processed",
                    command.getDocumentId());
            }

            sagaReplyPublisher.publishCompensated(
                command.getSagaId(),
                command.getSagaStep(),
                command.getCorrelationId()
            );

        } catch (Exception e) {
            log.error("Failed to compensate abbreviated tax invoice for saga {}: {}",
                command.getSagaId(), e.getMessage(), e);

            sagaReplyPublisher.publishFailure(
                command.getSagaId(),
                command.getSagaStep(),
                command.getCorrelationId(),
                "Compensation failed: " + e.getMessage()
            );
        }
    }
}
