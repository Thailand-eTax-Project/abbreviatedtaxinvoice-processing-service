package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging.dto;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wpanther.saga.domain.enums.SagaStep;
import com.wpanther.saga.domain.model.SagaCommand;
import lombok.Getter;

import java.time.Instant;
import java.util.UUID;

@Getter
public class ProcessAbbreviatedTaxInvoiceCommand extends SagaCommand {

    private static final long serialVersionUID = 1L;

    @JsonProperty("documentId")
    private final String documentId;

    @JsonProperty("xmlContent")
    private final String xmlContent;

    @JsonProperty("invoiceNumber")
    private final String invoiceNumber;

    @JsonCreator
    public ProcessAbbreviatedTaxInvoiceCommand(
            @JsonProperty("eventId") UUID eventId,
            @JsonProperty("occurredAt") Instant occurredAt,
            @JsonProperty("eventType") String eventType,
            @JsonProperty("version") int version,
            @JsonProperty("sagaId") String sagaId,
            @JsonProperty("sagaStep") SagaStep sagaStep,
            @JsonProperty("correlationId") String correlationId,
            @JsonProperty("documentId") String documentId,
            @JsonProperty("xmlContent") String xmlContent,
            @JsonProperty("invoiceNumber") String invoiceNumber) {
        super(eventId, occurredAt, eventType, version, sagaId, sagaStep, correlationId);
        this.documentId = documentId;
        this.xmlContent = xmlContent;
        this.invoiceNumber = invoiceNumber;
    }

    public ProcessAbbreviatedTaxInvoiceCommand(String sagaId, SagaStep sagaStep, String correlationId,
                                               String documentId, String xmlContent, String invoiceNumber) {
        super(sagaId, sagaStep, correlationId);
        this.documentId = documentId;
        this.xmlContent = xmlContent;
        this.invoiceNumber = invoiceNumber;
    }
}