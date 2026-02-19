package com.wpanther.abbreviatedtaxinvoice.processing.domain.model;

/**
 * Processing status for abbreviated tax invoices
 */
public enum ProcessingStatus {
    PENDING,
    PROCESSING,
    COMPLETED,
    FAILED,
    PDF_REQUESTED,
    PDF_GENERATED
}
