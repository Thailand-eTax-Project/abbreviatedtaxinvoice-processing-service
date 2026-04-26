package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging;

import com.wpanther.abbreviatedtaxinvoice.processing.application.port.in.CompensateAbbreviatedTaxInvoiceUseCase;
import com.wpanther.abbreviatedtaxinvoice.processing.application.port.in.ProcessAbbreviatedTaxInvoiceUseCase;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging.dto.CompensateAbbreviatedTaxInvoiceCommand;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging.dto.ProcessAbbreviatedTaxInvoiceCommand;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaCommandHandler {

    private final ProcessAbbreviatedTaxInvoiceUseCase processUseCase;
    private final CompensateAbbreviatedTaxInvoiceUseCase compensateUseCase;

    public void handleProcessCommand(ProcessAbbreviatedTaxInvoiceCommand command) {
        log.info("Handling ProcessAbbreviatedTaxInvoiceCommand for saga {} document {}",
            command.getSagaId(), command.getDocumentId());
        try {
            processUseCase.process(
                command.getDocumentId(),
                command.getXmlContent(),
                command.getSagaId(),
                command.getSagaStep(),
                command.getCorrelationId());
        } catch (ProcessAbbreviatedTaxInvoiceUseCase.AbbreviatedTaxInvoiceProcessingException e) {
            log.error("Failed to process abbreviated tax invoice for saga {}: {}",
                command.getSagaId(), e.toString(), e);
        }
    }

    public void handleCompensation(CompensateAbbreviatedTaxInvoiceCommand command) {
        log.info("Handling compensation for saga {} document {}",
            command.getSagaId(), command.getDocumentId());
        compensateUseCase.compensate(
            command.getDocumentId(),
            command.getSagaId(),
            command.getSagaStep(),
            command.getCorrelationId());
    }
}
