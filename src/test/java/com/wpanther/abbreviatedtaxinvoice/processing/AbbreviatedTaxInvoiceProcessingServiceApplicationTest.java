package com.wpanther.abbreviatedtaxinvoice.processing;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.context.ApplicationContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for AbbreviatedTaxInvoiceProcessingServiceApplication
 */
@SpringBootTest
@ActiveProfiles("test")
class AbbreviatedTaxInvoiceProcessingServiceApplicationTest {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    void contextLoads() {
        // Then
        assertNotNull(applicationContext, "Application context should load successfully");
    }

    @Test
    void testApplicationHasRequiredBeans() {
        // Then
        assertTrue(applicationContext.containsBean("abbreviatedTaxInvoiceProcessingService"),
            "Should have AbbreviatedTaxInvoiceProcessingService bean");
        assertTrue(applicationContext.containsBean("eventPublisher"),
            "Should have EventPublisher bean");
        assertTrue(applicationContext.containsBean("sagaRouteConfig"),
            "Should have SagaRouteConfig bean");
        assertTrue(applicationContext.containsBean("sagaCommandHandler"),
            "Should have SagaCommandHandler bean");
        assertTrue(applicationContext.containsBean("sagaReplyPublisher"),
            "Should have SagaReplyPublisher bean");
        assertTrue(applicationContext.containsBean("outboxService"),
            "Should have OutboxService bean from saga-commons");
    }

    @Test
    void testApplicationClassAnnotations() {
        // Then - verify the main application class has correct annotations
        assertNotNull(AbbreviatedTaxInvoiceProcessingServiceApplication.class.getAnnotation(SpringBootApplication.class),
            "Application class should have @SpringBootApplication annotation");
        assertNotNull(AbbreviatedTaxInvoiceProcessingServiceApplication.class.getAnnotation(EnableDiscoveryClient.class),
            "Application class should have @EnableDiscoveryClient annotation");
    }
}
