package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.out.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxEventRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OutboxCleanupSchedulerTest {

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Test
    void cleanPublishedEvents_deletesEventsOlderThanRetentionDays() {
        OutboxCleanupScheduler scheduler =
            new OutboxCleanupScheduler(outboxEventRepository, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(scheduler, "retentionDays", 7);
        ReflectionTestUtils.setField(scheduler, "cleanupCron", "0 0 2 * * *");
        scheduler.logConfiguration();

        when(outboxEventRepository.deletePublishedBefore(any())).thenReturn(42);

        scheduler.cleanPublishedEvents();

        ArgumentCaptor<Instant> cutoffCaptor = ArgumentCaptor.forClass(Instant.class);
        verify(outboxEventRepository).deletePublishedBefore(cutoffCaptor.capture());
        Instant cutoff = cutoffCaptor.getValue();
        Instant expectedApprox = Instant.now().minus(7, ChronoUnit.DAYS);
        assertTrue(Math.abs(cutoff.toEpochMilli() - expectedApprox.toEpochMilli()) < 5_000);
    }

    @Test
    void logConfiguration_throwsForRetentionDaysLessThanOne() {
        OutboxCleanupScheduler scheduler =
            new OutboxCleanupScheduler(outboxEventRepository, new SimpleMeterRegistry());
        ReflectionTestUtils.setField(scheduler, "retentionDays", 0);
        ReflectionTestUtils.setField(scheduler, "cleanupCron", "0 0 2 * * *");

        assertThrows(IllegalStateException.class, scheduler::logConfiguration);
    }

    @Test
    void cleanPublishedEvents_incrementsFailureCounterOnException() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OutboxCleanupScheduler scheduler = new OutboxCleanupScheduler(outboxEventRepository, registry);
        ReflectionTestUtils.setField(scheduler, "retentionDays", 7);
        ReflectionTestUtils.setField(scheduler, "cleanupCron", "0 0 2 * * *");
        scheduler.logConfiguration();

        when(outboxEventRepository.deletePublishedBefore(any()))
            .thenThrow(new RuntimeException("DB down"));

        scheduler.cleanPublishedEvents();

        double count = registry.counter("outbox.cleanup.failure").count();
        assertEquals(1.0, count);
    }
}
