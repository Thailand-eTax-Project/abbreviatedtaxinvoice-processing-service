package com.wpanther.abbreviatedtaxinvoice.processing.domain.event;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.wpanther.saga.domain.model.IntegrationEvent;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Event published when abbreviated tax invoice processing is completed
 */
@Getter
public class AbbreviatedTaxInvoiceProcessedEvent extends IntegrationEvent {

    private static final String EVENT_TYPE = "abbreviated.taxinvoice.processed";

    @JsonProperty("invoiceId")
    private final String invoiceId;

    @JsonProperty("invoiceNumber")
    private final String invoiceNumber;

    @JsonProperty("total")
    private final BigDecimal total;

    @JsonProperty("currency")
    private final String currency;

    @JsonProperty("correlationId")
    private final String correlationId;

    public AbbreviatedTaxInvoiceProcessedEvent(String invoiceId, String invoiceNumber, BigDecimal total,
                                               String currency, String correlationId) {
        super();
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.total = total;
        this.currency = currency;
        this.correlationId = correlationId;
    }

    @Override
    public String getEventType() {
        return EVENT_TYPE;
    }

    @JsonCreator
    public AbbreviatedTaxInvoiceProcessedEvent(
        @JsonProperty("eventId") UUID eventId,
        @JsonProperty("occurredAt") Instant occurredAt,
        @JsonProperty("eventType") String eventType,
        @JsonProperty("version") int version,
        @JsonProperty("invoiceId") String invoiceId,
        @JsonProperty("invoiceNumber") String invoiceNumber,
        @JsonProperty("total") BigDecimal total,
        @JsonProperty("currency") String currency,
        @JsonProperty("correlationId") String correlationId
    ) {
        super(eventId, occurredAt, eventType, version);
        this.invoiceId = invoiceId;
        this.invoiceNumber = invoiceNumber;
        this.total = total;
        this.currency = currency;
        this.correlationId = correlationId;
    }
}
