package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.persistence;

import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.AbbreviatedTaxInvoiceId;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessedAbbreviatedTaxInvoice;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.model.ProcessingStatus;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.repository.ProcessedAbbreviatedTaxInvoiceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of ProcessedAbbreviatedTaxInvoiceRepository using Spring Data JPA
 */
@Repository
@RequiredArgsConstructor
@Slf4j
public class ProcessedAbbreviatedTaxInvoiceRepositoryImpl implements ProcessedAbbreviatedTaxInvoiceRepository {

    private final JpaProcessedAbbreviatedTaxInvoiceRepository jpaRepository;
    private final ProcessedAbbreviatedTaxInvoiceMapper mapper;

    @Override
    @Transactional
    public ProcessedAbbreviatedTaxInvoice save(ProcessedAbbreviatedTaxInvoice invoice) {
        log.debug("Saving processed abbreviated tax invoice: {}", invoice.getInvoiceNumber());

        // Check if entity already exists (update case for state transitions)
        Optional<ProcessedAbbreviatedTaxInvoiceEntity> existingOpt =
            jpaRepository.findByIdWithDetails(invoice.getId().value());

        ProcessedAbbreviatedTaxInvoiceEntity saved;
        if (existingOpt.isPresent()) {
            // Update only mutable fields — children (parties, line items) don't change
            ProcessedAbbreviatedTaxInvoiceEntity existing = existingOpt.get();
            existing.setStatus(invoice.getStatus());
            existing.setErrorMessage(invoice.getErrorMessage());
            existing.setCompletedAt(invoice.getCompletedAt());
            saved = jpaRepository.save(existing);
        } else {
            // New entity — full mapping
            ProcessedAbbreviatedTaxInvoiceEntity entity = mapper.toEntity(invoice);
            saved = jpaRepository.save(entity);
        }

        log.info("Saved processed abbreviated tax invoice: {} with ID: {}", saved.getInvoiceNumber(), saved.getId());
        return mapper.toDomain(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedAbbreviatedTaxInvoice> findById(AbbreviatedTaxInvoiceId id) {
        log.debug("Finding abbreviated tax invoice by ID: {}", id);

        return jpaRepository.findByIdWithDetails(id.value())
            .map(entity -> {
                log.debug("Found abbreviated tax invoice: {}", entity.getInvoiceNumber());
                return mapper.toDomain(entity);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedAbbreviatedTaxInvoice> findByInvoiceNumber(String invoiceNumber) {
        log.debug("Finding abbreviated tax invoice by number: {}", invoiceNumber);

        return jpaRepository.findByInvoiceNumber(invoiceNumber)
            .map(entity -> {
                log.debug("Found abbreviated tax invoice with ID: {}", entity.getId());
                return mapper.toDomain(entity);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public List<ProcessedAbbreviatedTaxInvoice> findByStatus(ProcessingStatus status) {
        log.debug("Finding abbreviated tax invoices by status: {}", status);

        List<ProcessedAbbreviatedTaxInvoiceEntity> entities = jpaRepository.findByStatusWithDetails(status);
        log.debug("Found {} abbreviated tax invoices with status: {}", entities.size(), status);

        return entities.stream()
            .map(mapper::toDomain)
            .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<ProcessedAbbreviatedTaxInvoice> findBySourceInvoiceId(String sourceInvoiceId) {
        log.debug("Finding abbreviated tax invoice by source ID: {}", sourceInvoiceId);

        return jpaRepository.findBySourceInvoiceId(sourceInvoiceId)
            .map(entity -> {
                log.debug("Found abbreviated tax invoice: {}", entity.getInvoiceNumber());
                return mapper.toDomain(entity);
            });
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByInvoiceNumber(String invoiceNumber) {
        boolean exists = jpaRepository.existsByInvoiceNumber(invoiceNumber);
        log.debug("Abbreviated tax invoice number {} exists: {}", invoiceNumber, exists);
        return exists;
    }

    @Override
    @Transactional
    public void deleteById(AbbreviatedTaxInvoiceId id) {
        log.info("Deleting abbreviated tax invoice with ID: {}", id);
        jpaRepository.deleteById(id.value());
    }
}
