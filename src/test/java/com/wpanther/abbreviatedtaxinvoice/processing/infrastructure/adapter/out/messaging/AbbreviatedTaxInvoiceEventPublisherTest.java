package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.messaging;

import com.wpanther.abbreviatedtaxinvoice.processing.application.dto.event.AbbreviatedTaxInvoiceProcessedEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.event.AbbreviatedTaxInvoiceProcessedDomainEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.Money;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.messaging.EventPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AbbreviatedTaxInvoiceEventPublisherTest {

    @Mock
    private EventPublisher eventPublisher;

    @InjectMocks
    private AbbreviatedTaxInvoiceEventPublisher adapter;

    @Test
    void publish_translatesDomainEventToKafkaEvent() {
        Money total = Money.of(new BigDecimal("1000.00"), "THB");
        AbbreviatedTaxInvoiceProcessedDomainEvent domainEvent =
            AbbreviatedTaxInvoiceProcessedDomainEvent.of("doc-1", "INV-001", total, "saga-1", "corr-1");

        adapter.publish(domainEvent);

        ArgumentCaptor<AbbreviatedTaxInvoiceProcessedEvent> captor =
            ArgumentCaptor.forClass(AbbreviatedTaxInvoiceProcessedEvent.class);
        verify(eventPublisher).publishAbbreviatedTaxInvoiceProcessed(captor.capture());

        AbbreviatedTaxInvoiceProcessedEvent kafkaEvent = captor.getValue();
        assertEquals("doc-1", kafkaEvent.getDocumentId());
        assertEquals("INV-001", kafkaEvent.getInvoiceNumber());
        assertEquals(new BigDecimal("1000.00"), kafkaEvent.getTotal());
        assertEquals("THB", kafkaEvent.getCurrency());
        assertEquals("saga-1", kafkaEvent.getSagaId());
        assertEquals("corr-1", kafkaEvent.getCorrelationId());
    }

    @Test
    void publish_delegatesToEventPublisher() {
        Money total = Money.of(new BigDecimal("500.00"), "THB");
        AbbreviatedTaxInvoiceProcessedDomainEvent domainEvent =
            AbbreviatedTaxInvoiceProcessedDomainEvent.of("doc-2", "INV-002", total, "saga-2", "corr-2");

        adapter.publish(domainEvent);

        verify(eventPublisher, times(1)).publishAbbreviatedTaxInvoiceProcessed(any());
    }
}
