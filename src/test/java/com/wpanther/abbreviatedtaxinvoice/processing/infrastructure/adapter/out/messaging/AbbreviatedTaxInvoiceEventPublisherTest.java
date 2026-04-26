package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.messaging;

import com.wpanther.abbreviatedtaxinvoice.processing.application.dto.event.AbbreviatedTaxInvoiceProcessedEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.event.AbbreviatedTaxInvoiceProcessedDomainEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.Money;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config.HeaderSerializer;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class AbbreviatedTaxInvoiceEventPublisherTest {

    @Mock
    private OutboxService outboxService;
    @Mock
    private HeaderSerializer headerSerializer;

    private AbbreviatedTaxInvoiceEventPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new AbbreviatedTaxInvoiceEventPublisher(outboxService, headerSerializer,
            "abbreviated.taxinvoice.processed");
    }

    @Test
    void publish_translatesDomainEventToKafkaDto() {
        Money total = Money.of(new BigDecimal("1500.00"), "THB");
        AbbreviatedTaxInvoiceProcessedDomainEvent domainEvent =
            AbbreviatedTaxInvoiceProcessedDomainEvent.of(
                "doc-1", "INV-001", total, "saga-1", "corr-1");

        publisher.publish(domainEvent);

        ArgumentCaptor<AbbreviatedTaxInvoiceProcessedEvent> eventCaptor =
            ArgumentCaptor.forClass(AbbreviatedTaxInvoiceProcessedEvent.class);
        verify(outboxService).saveWithRouting(
            eventCaptor.capture(),
            eq("ProcessedAbbreviatedTaxInvoice"),
            eq("doc-1"),
            eq("abbreviated.taxinvoice.processed"),
            eq("doc-1"),
            any());

        AbbreviatedTaxInvoiceProcessedEvent kafkaEvent = eventCaptor.getValue();
        assertEquals("doc-1", kafkaEvent.getDocumentId());
        assertEquals("INV-001", kafkaEvent.getInvoiceNumber());
        assertEquals(new BigDecimal("1500.00"), kafkaEvent.getTotal());
        assertEquals("THB", kafkaEvent.getCurrency());
        assertEquals("corr-1", kafkaEvent.getCorrelationId());
    }
}
