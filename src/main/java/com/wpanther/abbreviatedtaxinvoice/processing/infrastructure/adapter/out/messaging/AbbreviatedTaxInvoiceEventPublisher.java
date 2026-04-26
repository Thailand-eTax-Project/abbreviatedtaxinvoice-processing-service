package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.messaging;

import com.wpanther.abbreviatedtaxinvoice.processing.application.dto.event.AbbreviatedTaxInvoiceProcessedEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.application.port.out.AbbreviatedTaxInvoiceEventPublishingPort;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.event.AbbreviatedTaxInvoiceProcessedDomainEvent;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config.HeaderSerializer;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config.KafkaTopicsProperties;
import com.wpanther.saga.infrastructure.outbox.OutboxService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Component
@Slf4j
public class AbbreviatedTaxInvoiceEventPublisher implements AbbreviatedTaxInvoiceEventPublishingPort {

    private static final String AGGREGATE_TYPE = "ProcessedAbbreviatedTaxInvoice";

    private final OutboxService outboxService;
    private final HeaderSerializer headerSerializer;
    private final String processedTopic;

    @Autowired
    public AbbreviatedTaxInvoiceEventPublisher(OutboxService outboxService,
                                               HeaderSerializer headerSerializer,
                                               KafkaTopicsProperties topics) {
        this(outboxService, headerSerializer, topics.getAbbreviatedTaxInvoiceProcessed());
    }

    AbbreviatedTaxInvoiceEventPublisher(OutboxService outboxService, HeaderSerializer headerSerializer,
                                        String processedTopic) {
        this.outboxService = outboxService;
        this.headerSerializer = headerSerializer;
        this.processedTopic = processedTopic;
    }

    @Override
    @Transactional(propagation = Propagation.MANDATORY)
    public void publish(AbbreviatedTaxInvoiceProcessedDomainEvent domainEvent) {
        AbbreviatedTaxInvoiceProcessedEvent kafkaEvent = new AbbreviatedTaxInvoiceProcessedEvent(
            domainEvent.documentId(),
            domainEvent.documentNumber(),
            domainEvent.total().amount(),
            domainEvent.total().currency(),
            domainEvent.correlationId()
        );

        Map<String, String> headers = Map.of(
            "correlationId", domainEvent.correlationId(),
            "documentNumber", domainEvent.documentNumber()
        );

        outboxService.saveWithRouting(
            kafkaEvent,
            AGGREGATE_TYPE,
            domainEvent.documentId(),
            processedTopic,
            domainEvent.documentId(),
            headerSerializer.toJson(headers)
        );

        log.info("Published AbbreviatedTaxInvoiceProcessedEvent to outbox: {}",
            domainEvent.documentNumber());
    }
}
