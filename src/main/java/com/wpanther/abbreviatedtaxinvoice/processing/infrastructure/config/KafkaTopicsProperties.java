package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Binds kafka.topics.* from application.yml into a typed properties class.
 * Registered via @EnableConfigurationProperties on the application class.
 */
@ConfigurationProperties(prefix = "kafka.topics")
@Getter
@Setter
public class KafkaTopicsProperties {

    private String sagaCommandAbbreviatedTaxInvoice = "saga.command.abbreviated-tax-invoice";
    private String sagaReplyAbbreviatedTaxInvoice = "saga.reply.abbreviated-tax-invoice";
    private String sagaCompensationAbbreviatedTaxInvoice = "saga.compensation.abbreviated-tax-invoice";
    private String abbreviatedTaxInvoiceProcessed = "abbreviated.taxinvoice.processed";
    private String dlq = "abbreviated.taxinvoice.processing.dlq";
}