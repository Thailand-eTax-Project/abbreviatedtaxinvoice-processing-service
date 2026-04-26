package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.messaging;

import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.messaging.dto.AbbreviatedTaxInvoiceReplyEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config.HeaderSerializer;
import com.wpanther.saga.domain.enums.ReplyStatus;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.domain.model.IntegrationEvent;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SagaReplyPublisherTest {

    @Mock
    private OutboxService outboxService;
    @Mock
    private HeaderSerializer headerSerializer;

    private SagaReplyPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new SagaReplyPublisher(outboxService, headerSerializer,
            "saga.reply.abbreviated-tax-invoice");
    }

    @Test
    void publishSuccess_savesOutboxEventWithSuccessStatus() {
        publisher.publishSuccess("saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");

        ArgumentCaptor<IntegrationEvent> eventCaptor = ArgumentCaptor.forClass(IntegrationEvent.class);
        verify(outboxService).saveWithRouting(
            eventCaptor.capture(), eq("ProcessedAbbreviatedTaxInvoice"),
            eq("saga-1"), eq("saga.reply.abbreviated-tax-invoice"),
            eq("saga-1"), any());

                AbbreviatedTaxInvoiceReplyEvent event = (AbbreviatedTaxInvoiceReplyEvent) eventCaptor.getValue();
        assertEquals(ReplyStatus.SUCCESS, event.getStatus());
        assertEquals("saga-1", event.getSagaId());
    }

    @Test
    void publishFailure_savesOutboxEventWithFailureStatusAndMessage() {
        publisher.publishFailure("saga-2", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-2", "err");

        ArgumentCaptor<IntegrationEvent> eventCaptor = ArgumentCaptor.forClass(IntegrationEvent.class);
        verify(outboxService).saveWithRouting(
            eventCaptor.capture(), any(), any(), any(), any(), any());

        AbbreviatedTaxInvoiceReplyEvent event = (AbbreviatedTaxInvoiceReplyEvent) eventCaptor.getValue();
        assertEquals(ReplyStatus.FAILURE, event.getStatus());
        assertEquals("err", event.getErrorMessage());
    }

    @Test
    void publishCompensated_savesOutboxEventWithCompensatedStatus() {
        publisher.publishCompensated("saga-3", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-3");

        ArgumentCaptor<IntegrationEvent> eventCaptor = ArgumentCaptor.forClass(IntegrationEvent.class);
        verify(outboxService).saveWithRouting(
            eventCaptor.capture(), any(), any(), any(), any(), any());

        AbbreviatedTaxInvoiceReplyEvent event = (AbbreviatedTaxInvoiceReplyEvent) eventCaptor.getValue();
        assertEquals(ReplyStatus.COMPENSATED, event.getStatus());
    }
}
