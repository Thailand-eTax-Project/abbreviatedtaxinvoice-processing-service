package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.messaging;

import com.wpanther.abbreviatedtaxinvoice.processing.application.dto.event.AbbreviatedTaxInvoiceProcessedEvent;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config.HeaderSerializer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class EventPublisher {

    private final OutboxService outboxService;
    private final HeaderSerializer headerSerializer;

    @Transactional(propagation = Propagation.MANDATORY)
    public void publishAbbreviatedTaxInvoiceProcessed(AbbreviatedTaxInvoiceProcessedEvent event) {
        Map<String, String> headers = Map.of(
            "correlationId", event.getCorrelationId(),
            "invoiceNumber", event.getInvoiceNumber()
        );

        outboxService.saveWithRouting(
            event,
            "ProcessedAbbreviatedTaxInvoice",
            event.getDocumentId(),
            "abbreviated.taxinvoice.processed",
            event.getDocumentId(),
            headerSerializer.toJson(headers)
        );

        log.info("Published AbbreviatedTaxInvoiceProcessedEvent to outbox: {}", event.getInvoiceNumber());
    }
}