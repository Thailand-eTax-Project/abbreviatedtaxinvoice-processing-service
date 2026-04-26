package com.wpanther.abbreviatedtaxinvoice.processing.domain.port.out;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.AbbreviatedTaxInvoiceId;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessedAbbreviatedTaxInvoice;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessingStatus;

import java.util.List;
import java.util.Optional;

public interface ProcessedAbbreviatedTaxInvoiceRepository {
    ProcessedAbbreviatedTaxInvoice save(ProcessedAbbreviatedTaxInvoice invoice);
    Optional<ProcessedAbbreviatedTaxInvoice> findById(AbbreviatedTaxInvoiceId id);
    Optional<ProcessedAbbreviatedTaxInvoice> findByInvoiceNumber(String invoiceNumber);
    List<ProcessedAbbreviatedTaxInvoice> findByStatus(ProcessingStatus status);
    Optional<ProcessedAbbreviatedTaxInvoice> findBySourceInvoiceId(String sourceInvoiceId);
    boolean existsByInvoiceNumber(String invoiceNumber);
    void deleteById(AbbreviatedTaxInvoiceId id);
}
