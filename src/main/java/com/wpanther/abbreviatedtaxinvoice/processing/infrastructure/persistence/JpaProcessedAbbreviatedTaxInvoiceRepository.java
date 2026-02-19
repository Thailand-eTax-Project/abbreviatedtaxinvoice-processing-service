package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.persistence;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Spring Data JPA repository for ProcessedAbbreviatedTaxInvoiceEntity
 */
@Repository
public interface JpaProcessedAbbreviatedTaxInvoiceRepository
        extends JpaRepository<ProcessedAbbreviatedTaxInvoiceEntity, UUID> {

    /**
     * Find by invoice number
     */
    Optional<ProcessedAbbreviatedTaxInvoiceEntity> findByInvoiceNumber(String invoiceNumber);

    /**
     * Find by source invoice ID
     */
    Optional<ProcessedAbbreviatedTaxInvoiceEntity> findBySourceInvoiceId(String sourceInvoiceId);

    /**
     * Find by processing status
     */
    List<ProcessedAbbreviatedTaxInvoiceEntity> findByStatus(ProcessingStatus status);

    /**
     * Check if invoice number exists
     */
    boolean existsByInvoiceNumber(String invoiceNumber);

    /**
     * Find invoice with parties and line items eagerly loaded
     */
    @Query("SELECT i FROM ProcessedAbbreviatedTaxInvoiceEntity i " +
           "LEFT JOIN FETCH i.parties " +
           "LEFT JOIN FETCH i.lineItems " +
           "WHERE i.id = :id")
    Optional<ProcessedAbbreviatedTaxInvoiceEntity> findByIdWithDetails(@Param("id") UUID id);

    /**
     * Find invoices by status with details
     */
    @Query("SELECT DISTINCT i FROM ProcessedAbbreviatedTaxInvoiceEntity i " +
           "LEFT JOIN FETCH i.parties " +
           "LEFT JOIN FETCH i.lineItems " +
           "WHERE i.status = :status")
    List<ProcessedAbbreviatedTaxInvoiceEntity> findByStatusWithDetails(@Param("status") ProcessingStatus status);
}
