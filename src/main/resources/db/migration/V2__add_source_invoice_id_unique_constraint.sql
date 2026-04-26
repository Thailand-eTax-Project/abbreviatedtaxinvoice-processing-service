-- ============================================================
-- V2: Add named UNIQUE constraint on source_invoice_id
-- ============================================================
--
-- Adds a named UNIQUE constraint on source_invoice_id so that
-- AbbreviatedTaxInvoiceProcessingService.isSourceInvoiceIdViolation() can detect
-- concurrent-insert race conditions by constraint name and SQLState "23505".
--
-- The plain index (idx_abbr_tax_source_invoice_id) is dropped because a UNIQUE
-- constraint creates its own index; keeping both wastes space and slows writes.

ALTER TABLE processed_abbreviated_tax_invoices
    ADD CONSTRAINT uq_processed_abbreviated_tax_invoices_source_invoice_id
    UNIQUE (source_invoice_id);

DROP INDEX IF EXISTS idx_abbr_tax_source_invoice_id;
