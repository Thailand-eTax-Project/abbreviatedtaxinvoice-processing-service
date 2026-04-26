package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.messaging;

import com.wpanther.abbreviatedtaxinvoice.processing.application.dto.event.AbbreviatedTaxInvoiceProcessedEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.application.port.out.AbbreviatedTaxInvoiceEventPublishingPort;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.event.AbbreviatedTaxInvoiceProcessedDomainEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.messaging.EventPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AbbreviatedTaxInvoiceEventPublisher implements AbbreviatedTaxInvoiceEventPublishingPort {

    private final EventPublisher eventPublisher;

    @Override
    public void publish(AbbreviatedTaxInvoiceProcessedDomainEvent domainEvent) {
        AbbreviatedTaxInvoiceProcessedEvent kafkaEvent = new AbbreviatedTaxInvoiceProcessedEvent(
            domainEvent.documentId(),
            domainEvent.documentNumber(),
            domainEvent.total().amount(),
            domainEvent.total().currency(),
            domainEvent.sagaId(),
            domainEvent.correlationId()
        );
        eventPublisher.publishAbbreviatedTaxInvoiceProcessed(kafkaEvent);
    }
}
