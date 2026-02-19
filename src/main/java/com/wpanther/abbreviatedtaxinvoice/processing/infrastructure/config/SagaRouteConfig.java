package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.config;

import com.wpanther.abbreviatedtaxinvoice.processing.application.service.SagaCommandHandler;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.event.CompensateAbbreviatedTaxInvoiceCommand;
import com.wpanther.abbreviatedtaxinvoice.processing.domain.event.ProcessAbbreviatedTaxInvoiceCommand;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Apache Camel routes for saga command and compensation consumers.
 * Consumes ProcessAbbreviatedTaxInvoiceCommand and CompensateAbbreviatedTaxInvoiceCommand
 * from their respective Kafka topics.
 */
@Component
@Slf4j
public class SagaRouteConfig extends RouteBuilder {

    private final SagaCommandHandler sagaCommandHandler;

    @Value("${app.kafka.bootstrap-servers}")
    private String kafkaBrokers;

    @Value("${app.kafka.topics.saga-command-abbreviated-tax-invoice}")
    private String sagaCommandTopic;

    @Value("${app.kafka.topics.saga-compensation-abbreviated-tax-invoice}")
    private String sagaCompensationTopic;

    @Value("${app.kafka.topics.dlq:abbreviated.taxinvoice.processing.dlq}")
    private String dlqTopic;

    public SagaRouteConfig(SagaCommandHandler sagaCommandHandler) {
        this.sagaCommandHandler = sagaCommandHandler;
    }

    @Override
    public void configure() throws Exception {

        // Global error handler - Dead Letter Channel with retries
        errorHandler(deadLetterChannel("kafka:" + dlqTopic + "?brokers=" + kafkaBrokers)
            .maximumRedeliveries(3)
            .redeliveryDelay(1000)
            .useExponentialBackOff()
            .backOffMultiplier(2)
            .maximumRedeliveryDelay(10000)
            .logExhausted(true)
            .logStackTrace(true));

        // ============================================================
        // CONSUMER ROUTE: saga.command.abbreviated-tax-invoice (from orchestrator)
        // ============================================================
        from("kafka:" + sagaCommandTopic
                + "?brokers=" + kafkaBrokers
                + "&groupId=abbreviatedtaxinvoice-processing-service"
                + "&autoOffsetReset=earliest"
                + "&autoCommitEnable=false"
                + "&breakOnFirstError=true"
                + "&maxPollRecords=100"
                + "&consumersCount=3")
            .routeId("saga-command-consumer")
            .log("Received saga command from Kafka: partition=${header[kafka.PARTITION]}, offset=${header[kafka.OFFSET]}")
            .unmarshal().json(JsonLibrary.Jackson, ProcessAbbreviatedTaxInvoiceCommand.class)
            .process(exchange -> {
                ProcessAbbreviatedTaxInvoiceCommand cmd =
                    exchange.getIn().getBody(ProcessAbbreviatedTaxInvoiceCommand.class);
                log.info("Processing saga command for saga: {}, invoice: {}",
                    cmd.getSagaId(), cmd.getInvoiceNumber());
                sagaCommandHandler.handleProcessCommand(cmd);
            })
            .log("Successfully processed saga command");

        // ============================================================
        // CONSUMER ROUTE: saga.compensation.abbreviated-tax-invoice (from orchestrator)
        // ============================================================
        from("kafka:" + sagaCompensationTopic
                + "?brokers=" + kafkaBrokers
                + "&groupId=abbreviatedtaxinvoice-processing-service"
                + "&autoOffsetReset=earliest"
                + "&autoCommitEnable=false"
                + "&breakOnFirstError=true"
                + "&maxPollRecords=100"
                + "&consumersCount=3")
            .routeId("saga-compensation-consumer")
            .log("Received compensation command from Kafka: partition=${header[kafka.PARTITION]}, offset=${header[kafka.OFFSET]}")
            .unmarshal().json(JsonLibrary.Jackson, CompensateAbbreviatedTaxInvoiceCommand.class)
            .process(exchange -> {
                CompensateAbbreviatedTaxInvoiceCommand cmd =
                    exchange.getIn().getBody(CompensateAbbreviatedTaxInvoiceCommand.class);
                log.info("Processing compensation for saga: {}, document: {}",
                    cmd.getSagaId(), cmd.getDocumentId());
                sagaCommandHandler.handleCompensation(cmd);
            })
            .log("Successfully processed compensation command");
    }
}
