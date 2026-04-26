package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.messaging;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.event.AbbreviatedTaxInvoiceReplyEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config.HeaderSerializer;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class SagaReplyPublisher {

    private static final String REPLY_TOPIC = "saga.reply.abbreviated-tax-invoice";
    private static final String AGGREGATE_TYPE = "ProcessedAbbreviatedTaxInvoice";

    private final OutboxService outboxService;
    private final HeaderSerializer headerSerializer;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishSuccess(String sagaId, SagaStep sagaStep, String correlationId) {
        AbbreviatedTaxInvoiceReplyEvent reply =
            AbbreviatedTaxInvoiceReplyEvent.success(sagaId, sagaStep, correlationId);

        Map<String, String> headers = Map.of(
            "sagaId", sagaId,
            "correlationId", correlationId,
            "status", "SUCCESS"
        );

        outboxService.saveWithRouting(
            reply,
            AGGREGATE_TYPE,
            sagaId,
            REPLY_TOPIC,
            sagaId,
            headerSerializer.toJson(headers)
        );

        log.info("Published SUCCESS saga reply for saga {} step {}", sagaId, sagaStep);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishFailure(String sagaId, SagaStep sagaStep, String correlationId, String errorMessage) {
        AbbreviatedTaxInvoiceReplyEvent reply =
            AbbreviatedTaxInvoiceReplyEvent.failure(sagaId, sagaStep, correlationId, errorMessage);

        Map<String, String> headers = Map.of(
            "sagaId", sagaId,
            "correlationId", correlationId,
            "status", "FAILURE"
        );

        outboxService.saveWithRouting(
            reply,
            AGGREGATE_TYPE,
            sagaId,
            REPLY_TOPIC,
            sagaId,
            headerSerializer.toJson(headers)
        );

        log.info("Published FAILURE saga reply for saga {} step {}: {}", sagaId, sagaStep, errorMessage);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishCompensated(String sagaId, SagaStep sagaStep, String correlationId) {
        AbbreviatedTaxInvoiceReplyEvent reply =
            AbbreviatedTaxInvoiceReplyEvent.compensated(sagaId, sagaStep, correlationId);

        Map<String, String> headers = Map.of(
            "sagaId", sagaId,
            "correlationId", correlationId,
            "status", "COMPENSATED"
        );

        outboxService.saveWithRouting(
            reply,
            AGGREGATE_TYPE,
            sagaId,
            REPLY_TOPIC,
            sagaId,
            headerSerializer.toJson(headers)
        );

        log.info("Published COMPENSATED saga reply for saga {} step {}", sagaId, sagaStep);
    }
}