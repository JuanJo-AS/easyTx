package io.easytx.annotation;

import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;

public class TransactionConfiguration {
    private Propagation propagation;
    private Isolation isolation;

    public TransactionConfiguration(TxRead txRead) {
        this.propagation = txRead.propagation();
        this.isolation = txRead.isolation();
    }

    public TransactionConfiguration(TxWrite txWrite) {
        this.propagation = txWrite.propagation();
        this.isolation = txWrite.isolation();
    }

    public TransactionConfiguration(Propagation propagation, Isolation isolation) {
        this.propagation = propagation;
        this.isolation = isolation;
    }

    public Propagation getPropagation() {
        return propagation;
    }

    public void setPropagation(Propagation propagation) {
        this.propagation = propagation;
    }

    public Isolation getIsolation() {
        return isolation;
    }

    public void setIsolation(Isolation isolation) {
        this.isolation = isolation;
    }

}
