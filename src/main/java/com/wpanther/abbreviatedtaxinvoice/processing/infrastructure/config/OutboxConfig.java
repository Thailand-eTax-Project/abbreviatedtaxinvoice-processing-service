package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config;

import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.persistence.outbox.JpaOutboxEventRepository;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.persistence.outbox.SpringDataOutboxRepository;
import com.wpanther.saga.domain.outbox.OutboxEventRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OutboxConfig {

    @Bean
    @ConditionalOnMissingBean(OutboxEventRepository.class)
    public OutboxEventRepository outboxEventRepository(SpringDataOutboxRepository springRepository) {
        return new JpaOutboxEventRepository(springRepository);
    }
}
