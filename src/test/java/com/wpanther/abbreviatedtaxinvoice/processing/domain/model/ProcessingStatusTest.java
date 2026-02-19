package com.wpanther.abbreviatedtaxinvoice.processing.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for ProcessingStatus enum
 */
class ProcessingStatusTest {

    @Test
    void testAllStatusValues() {
        ProcessingStatus[] statuses = ProcessingStatus.values();

        assertEquals(6, statuses.length);
        assertArrayEquals(
            new ProcessingStatus[]{
                ProcessingStatus.PENDING,
                ProcessingStatus.PROCESSING,
                ProcessingStatus.COMPLETED,
                ProcessingStatus.FAILED,
                ProcessingStatus.PDF_REQUESTED,
                ProcessingStatus.PDF_GENERATED
            },
            statuses
        );
    }

    @Test
    void testValueOf() {
        assertEquals(ProcessingStatus.PENDING, ProcessingStatus.valueOf("PENDING"));
        assertEquals(ProcessingStatus.PROCESSING, ProcessingStatus.valueOf("PROCESSING"));
        assertEquals(ProcessingStatus.COMPLETED, ProcessingStatus.valueOf("COMPLETED"));
        assertEquals(ProcessingStatus.PDF_REQUESTED, ProcessingStatus.valueOf("PDF_REQUESTED"));
        assertEquals(ProcessingStatus.PDF_GENERATED, ProcessingStatus.valueOf("PDF_GENERATED"));
    }

    @Test
    void testValueOfInvalid() {
        assertThrows(IllegalArgumentException.class, () ->
            ProcessingStatus.valueOf("INVALID_STATUS")
        );
    }

    @Test
    void testEnumEquality() {
        assertSame(ProcessingStatus.PENDING, ProcessingStatus.valueOf("PENDING"));
        assertSame(ProcessingStatus.COMPLETED, ProcessingStatus.valueOf("COMPLETED"));
    }
}
