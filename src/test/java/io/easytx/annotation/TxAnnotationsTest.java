package io.easytx.annotation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.transaction.support.TransactionSynchronization;
import io.easytx.service.AnnotatedService;
import io.easytx.service.TransactionService;

@SpringBootTest
class TxAnnotationsTest {

    @Autowired
    private AnnotatedService annotatedService;

    @MockitoSpyBean
    private TransactionService transactionService;

    @Test
    void testTxReadAnnotation() {
        annotatedService.doReadOperation();
        verify(transactionService, atLeastOnce()).withReadTransaction(any(), any());
    }

    @Test
    void testTxWriteAnnotation() {
        annotatedService.doWriteOperation();
        verify(transactionService, atLeastOnce()).withWriteTransaction(any(), any());
    }

    @Test
    void testTxWriteAnnotationOpensTransaction() {
        assertTrue(annotatedService.readIsActive(),
                "La transacción debería estar activa con @TxWrite");
    }

    @Test
    void testTxReadAnnotationOpensTransaction() {
        assertTrue(annotatedService.writeIsActive(),
                "La transacción debería estar activa con @TxRead");
    }

    @Test
    void testTxWriteAnnotationClosesTransaction() {
        AtomicBoolean closed = new AtomicBoolean(false);

        annotatedService.doWriteWithAfterCompletion(closed);

        assertTrue(closed.get(), "La transacción con @TxWrite debería cerrarse al finalizar");
    }

    @Test
    void testTxReadAnnotationClosesTransaction() {
        AtomicBoolean closed = new AtomicBoolean(false);
        annotatedService.doReadWithAfterCompletion(closed);
        assertTrue(closed.get(), "La transacción con @TxRead debería cerrarse al finalizar");
    }

    @Test
    void testTxWriteCommitted() {
        AtomicInteger status = new AtomicInteger(-1);
        annotatedService.doWriteWithStatus(status, false);
        assertEquals(TransactionSynchronization.STATUS_COMMITTED, status.get(),
                "La transacción debería terminar con COMMIT");
    }

    @Test
    void testTxWriteRolledBack() {
        AtomicInteger status = new AtomicInteger(-1);
        assertThrows(RuntimeException.class,
                () -> annotatedService.doWriteWithStatus(status, true));
        assertEquals(TransactionSynchronization.STATUS_ROLLED_BACK, status.get(),
                "La transacción debería terminar con ROLLBACK");
    }
}
