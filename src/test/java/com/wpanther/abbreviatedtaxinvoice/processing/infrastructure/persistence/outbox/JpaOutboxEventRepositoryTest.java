package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.persistence.outbox;

import com.wpanther.saga.domain.outbox.OutboxEvent;
import com.wpanther.saga.domain.outbox.OutboxStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class JpaOutboxEventRepositoryTest {

    @Mock
    private SpringDataOutboxRepository springRepository;

    private JpaOutboxEventRepository repository;

    private OutboxEventEntity buildEntity(UUID id) {
        return OutboxEventEntity.builder()
            .id(id)
            .aggregateType("ProcessedAbbreviatedTaxInvoice")
            .aggregateId("abr-inv-123")
            .eventType("AbbreviatedTaxInvoiceProcessedEvent")
            .payload("{\"key\":\"value\"}")
            .createdAt(Instant.now())
            .status(OutboxStatus.PENDING)
            .retryCount(0)
            .build();
    }

    @BeforeEach
    void setUp() {
        repository = new JpaOutboxEventRepository(springRepository);
    }

    @Test
    void testSave() {
        UUID id = UUID.randomUUID();
        OutboxEventEntity entity = buildEntity(id);
        when(springRepository.save(any(OutboxEventEntity.class))).thenReturn(entity);

        OutboxEvent domain = entity.toDomain();
        OutboxEvent result = repository.save(domain);

        assertNotNull(result);
        assertEquals(id, result.getId());
        assertEquals("ProcessedAbbreviatedTaxInvoice", result.getAggregateType());
        verify(springRepository).save(any(OutboxEventEntity.class));
    }

    @Test
    void testFindById_Found() {
        UUID id = UUID.randomUUID();
        OutboxEventEntity entity = buildEntity(id);
        when(springRepository.findById(id)).thenReturn(Optional.of(entity));

        Optional<OutboxEvent> result = repository.findById(id);

        assertTrue(result.isPresent());
        assertEquals(id, result.get().getId());
    }

    @Test
    void testFindById_NotFound() {
        UUID id = UUID.randomUUID();
        when(springRepository.findById(id)).thenReturn(Optional.empty());

        Optional<OutboxEvent> result = repository.findById(id);

        assertFalse(result.isPresent());
    }

    @Test
    void testFindPendingEvents() {
        UUID id = UUID.randomUUID();
        List<OutboxEventEntity> entities = List.of(buildEntity(id));
        when(springRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
            .thenReturn(entities);

        List<OutboxEvent> result = repository.findPendingEvents(10);

        assertEquals(1, result.size());
        assertEquals(id, result.get(0).getId());
    }

    @Test
    void testFindPendingEvents_Empty() {
        when(springRepository.findByStatusOrderByCreatedAtAsc(eq(OutboxStatus.PENDING), any(Pageable.class)))
            .thenReturn(List.of());

        List<OutboxEvent> result = repository.findPendingEvents(10);

        assertTrue(result.isEmpty());
    }

    @Test
    void testFindFailedEvents() {
        UUID id = UUID.randomUUID();
        OutboxEventEntity entity = buildEntity(id);
        entity.setStatus(OutboxStatus.FAILED);
        when(springRepository.findFailedEventsOrderByCreatedAtAsc(any(Pageable.class)))
            .thenReturn(List.of(entity));

        List<OutboxEvent> result = repository.findFailedEvents(10);

        assertEquals(1, result.size());
        assertEquals(OutboxStatus.FAILED, result.get(0).getStatus());
    }

    @Test
    void testDeletePublishedBefore() {
        Instant before = Instant.now();
        when(springRepository.deletePublishedBefore(before)).thenReturn(5);

        int result = repository.deletePublishedBefore(before);

        assertEquals(5, result);
        verify(springRepository).deletePublishedBefore(before);
    }

    @Test
    void testFindByAggregate() {
        UUID id = UUID.randomUUID();
        List<OutboxEventEntity> entities = List.of(buildEntity(id));
        when(springRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            "ProcessedAbbreviatedTaxInvoice", "abr-inv-123"))
            .thenReturn(entities);

        List<OutboxEvent> result = repository.findByAggregate("ProcessedAbbreviatedTaxInvoice", "abr-inv-123");

        assertEquals(1, result.size());
        assertEquals("ProcessedAbbreviatedTaxInvoice", result.get(0).getAggregateType());
    }

    @Test
    void testFindByAggregate_Empty() {
        when(springRepository.findByAggregateTypeAndAggregateIdOrderByCreatedAtAsc(
            "ProcessedAbbreviatedTaxInvoice", "unknown"))
            .thenReturn(List.of());

        List<OutboxEvent> result = repository.findByAggregate("ProcessedAbbreviatedTaxInvoice", "unknown");

        assertTrue(result.isEmpty());
    }
}
