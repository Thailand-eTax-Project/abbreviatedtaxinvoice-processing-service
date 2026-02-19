package com.wpanther.abbreviatedtaxinvoice.processing.domain.repository;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.AbbreviatedTaxInvoiceId;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessedAbbreviatedTaxInvoice;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessingStatus;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for ProcessedAbbreviatedTaxInvoice aggregate.
 * Works with domain objects; implementations handle entity mapping.
 */
public interface ProcessedAbbreviatedTaxInvoiceRepository {

    /**
     * Save a processed abbreviated tax invoice
     */
    ProcessedAbbreviatedTaxInvoice save(ProcessedAbbreviatedTaxInvoice invoice);

    /**
     * Find invoice by ID
     */
    Optional<ProcessedAbbreviatedTaxInvoice> findById(AbbreviatedTaxInvoiceId id);

    /**
     * Find invoice by invoice number
     */
    Optional<ProcessedAbbreviatedTaxInvoice> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find invoices by status
     */
    List<ProcessedAbbreviatedTaxInvoice> findByStatus(ProcessingStatus status);

    /**
     * Find invoice by source invoice ID
     */
    Optional<ProcessedAbbreviatedTaxInvoice> findBySourceInvoiceId(String sourceInvoiceId);

    /**
     * Check if invoice number already exists
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Delete invoice by ID
     */
    void deleteById(AbbreviatedTaxInvoiceId id);
}
