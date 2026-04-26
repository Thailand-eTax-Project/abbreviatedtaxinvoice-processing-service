package com.wpanther.abbreviatedtaxinvoice.processing;

import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config.KafkaTopicsProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Abbreviated Tax Invoice Processing Service Application
 *
 * Processes AbbreviatedTaxInvoice XML documents as part of the saga orchestration pipeline.
 * Consumes commands from saga.command.abbreviated-tax-invoice topic and publishes
 * replies to saga.reply.abbreviated-tax-invoice topic.
 */
@SpringBootApplication
@EnableDiscoveryClient
@EnableConfigurationProperties(KafkaTopicsProperties.class)
public class AbbreviatedTaxInvoiceProcessingServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(AbbreviatedTaxInvoiceProcessingServiceApplication.class, args);
    }
}
