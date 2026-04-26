package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.messaging;

import com.wpanther.abbreviatedtaxinvoice.processing.application.port.out.SagaReplyPort;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.messaging.dto.AbbreviatedTaxInvoiceReplyEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config.HeaderSerializer;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config.KafkaTopicsProperties;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@Slf4j
public class SagaReplyPublisher implements SagaReplyPort {

    private static final String AGGREGATE_TYPE = "ProcessedAbbreviatedTaxInvoice";

    private final OutboxService outboxService;
    private final HeaderSerializer headerSerializer;
    private final String replyTopic;

    @Autowired
    public SagaReplyPublisher(OutboxService outboxService, HeaderSerializer headerSerializer,
                               KafkaTopicsProperties topics) {
        this(outboxService, headerSerializer, topics.getSagaReplyAbbreviatedTaxInvoice());
    }

    SagaReplyPublisher(OutboxService outboxService, HeaderSerializer headerSerializer, String replyTopic) {
        this.outboxService = outboxService;
        this.headerSerializer = headerSerializer;
        this.replyTopic = replyTopic;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishSuccess(String sagaId, SagaStep sagaStep, String correlationId) {
        AbbreviatedTaxInvoiceReplyEvent reply =
            AbbreviatedTaxInvoiceReplyEvent.success(sagaId, sagaStep, correlationId);
        Map<String, String> headers = Map.of(
            "sagaId", sagaId, "correlationId", correlationId, "status", "SUCCESS");
        outboxService.saveWithRouting(reply, AGGREGATE_TYPE, sagaId, replyTopic, sagaId,
            headerSerializer.toJson(headers));
        log.info("Published SUCCESS saga reply for saga {} step {}", sagaId, sagaStep);
    }

    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void publishFailure(String sagaId, SagaStep sagaStep, String correlationId,
                               String errorMessage) {
        AbbreviatedTaxInvoiceReplyEvent reply =
            AbbreviatedTaxInvoiceReplyEvent.failure(sagaId, sagaStep, correlationId, errorMessage);
        Map<String, String> headers = Map.of(
            "sagaId", sagaId, "correlationId", correlationId, "status", "FAILURE");
        outboxService.saveWithRouting(reply, AGGREGATE_TYPE, sagaId, replyTopic, sagaId,
            headerSerializer.toJson(headers));
        log.info("Published FAILURE saga reply for saga {} step {}: {}", sagaId, sagaStep, errorMessage);
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publishCompensated(String sagaId, SagaStep sagaStep, String correlationId) {
        AbbreviatedTaxInvoiceReplyEvent reply =
            AbbreviatedTaxInvoiceReplyEvent.compensated(sagaId, sagaStep, correlationId);
        Map<String, String> headers = Map.of(
            "sagaId", sagaId, "correlationId", correlationId, "status", "COMPENSATED");
        outboxService.saveWithRouting(reply, AGGREGATE_TYPE, sagaId, replyTopic, sagaId,
            headerSerializer.toJson(headers));
        log.info("Published COMPENSATED saga reply for saga {} step {}", sagaId, sagaStep);
    }
}
