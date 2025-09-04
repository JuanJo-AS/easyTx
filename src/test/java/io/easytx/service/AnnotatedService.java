package io.easytx.service;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import io.easytx.annotation.TxRead;
import io.easytx.annotation.TxWrite;

@Service
public class AnnotatedService {
    @TxRead
    public String doReadOperation() {
        return "READ_OK";
    }

    @TxWrite
    public String doWriteOperation() {
        return "WRITE_OK";
    }

    @TxRead
    public boolean readIsActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    @TxWrite
    public boolean writeIsActive() {
        return TransactionSynchronizationManager.isActualTransactionActive();
    }

    @TxWrite
    public void doWriteWithAfterCompletion(AtomicBoolean closed) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                closed.set(true);
            }
        });
    }

    @TxRead
    public void doReadWithAfterCompletion(AtomicBoolean closed) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                closed.set(true);
            }
        });
    }

    @TxWrite
    public void doWriteWithStatus(AtomicInteger statusHolder, boolean rollback) {
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                statusHolder.set(status);
            }
        });

        if (rollback) {
            throw new RuntimeException("Forzando rollback");
        }
    }
}
