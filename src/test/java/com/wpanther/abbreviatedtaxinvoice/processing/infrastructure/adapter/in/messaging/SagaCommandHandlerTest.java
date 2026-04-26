package com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging;

import com.wpanther.abbreviatedtaxinvoice.processing.application.port.in.CompensateAbbreviatedTaxInvoiceUseCase;
import com.wpanther.abbreviatedtaxinvoice.processing.application.port.in.ProcessAbbreviatedTaxInvoiceUseCase;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging.dto.CompensateAbbreviatedTaxInvoiceCommand;
import com.wpanther.abbreviatedtaxinvoice.processing.infrastructure.adapter.in.messaging.dto.ProcessAbbreviatedTaxInvoiceCommand;
import com.wpanther.saga.domain.enums.SagaStep;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SagaCommandHandlerTest {

    @Mock private ProcessAbbreviatedTaxInvoiceUseCase processUseCase;
    @Mock private CompensateAbbreviatedTaxInvoiceUseCase compensateUseCase;

    @InjectMocks
    private SagaCommandHandler handler;

    private ProcessAbbreviatedTaxInvoiceCommand processCmd() {
        return new ProcessAbbreviatedTaxInvoiceCommand(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1",
            "doc-1", "<xml/>", "ABR-001");
    }

    private CompensateAbbreviatedTaxInvoiceCommand compensateCmd() {
        return new CompensateAbbreviatedTaxInvoiceCommand(
            "saga-1", SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1",
            "process-abbreviated-tax-invoice", "doc-1", "abbreviated-tax-invoice");
    }

    @Test
    void handleProcessCommand_success_delegatesToUseCase() throws Exception {
        handler.handleProcessCommand(processCmd());

        verify(processUseCase).process("doc-1", "<xml/>", "saga-1",
            SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");
    }

    @Test
    void handleProcessCommand_processingException_swallowed_offsetCommitted() throws Exception {
        doThrow(new ProcessAbbreviatedTaxInvoiceUseCase.AbbreviatedTaxInvoiceProcessingException("boom"))
            .when(processUseCase).process(anyString(), anyString(), anyString(), any(), anyString());

        handler.handleProcessCommand(processCmd());
    }

    @Test
    void handleProcessCommand_unexpectedException_propagates() throws Exception {
        doThrow(new RuntimeException("DB down"))
            .when(processUseCase).process(anyString(), anyString(), anyString(), any(), anyString());

        assertThrows(RuntimeException.class, () -> handler.handleProcessCommand(processCmd()));
    }

    @Test
    void handleCompensation_delegatesToUseCase() {
        handler.handleCompensation(compensateCmd());

        verify(compensateUseCase).compensate("doc-1", "saga-1",
            SagaStep.PROCESS_ABBREVIATED_TAX_INVOICE, "corr-1");
    }

    @Test
    void handleCompensation_compensationException_propagates() {
        doThrow(new CompensateAbbreviatedTaxInvoiceUseCase.AbbreviatedTaxInvoiceCompensationException("fail"))
            .when(compensateUseCase).compensate(anyString(), anyString(), any(), anyString());

        assertThrows(CompensateAbbreviatedTaxInvoiceUseCase.AbbreviatedTaxInvoiceCompensationException.class,
            () -> handler.handleCompensation(compensateCmd()));
    }
}
