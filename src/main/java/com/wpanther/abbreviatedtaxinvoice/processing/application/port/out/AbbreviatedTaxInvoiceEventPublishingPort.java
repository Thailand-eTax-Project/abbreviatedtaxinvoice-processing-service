package com.wpanther.abbreviatedtaxinvoice.processing.application.port.out;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.event.AbbreviatedTaxInvoiceProcessedDomainEvent;

public interface AbbreviatedTaxInvoiceEventPublishingPort {
    void publish(AbbreviatedTaxInvoiceProcessedDomainEvent event);
}
