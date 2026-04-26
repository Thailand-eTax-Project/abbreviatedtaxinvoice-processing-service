package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Binds kafka.topics.* from application.yml into a typed properties class.
 */
@Component
@ConfigurationProperties(prefix = "kafka.topics")
@Getter
@Setter
public class KafkaTopicsProperties {

    private String sagaCommandAbbreviatedTaxInvoice = "saga.command.abbreviated-tax-invoice";
    private String sagaReplyAbbreviatedTaxInvoice = "saga.reply.abbreviated-tax-invoice";
    private String sagaCompensationAbbreviatedTaxInvoice = "saga.compensation.abbreviated-tax-invoice";
    private String abbreviatedTaxInvoiceProcessed = "abbreviated.taxinvoice.processed";
}