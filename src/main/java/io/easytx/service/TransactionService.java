package io.easytx.service;

import java.util.function.Supplier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.support.TransactionTemplate;
import io.easytx.annotation.TransactionConfiguration;

@Service
public class TransactionService {

    private final TransactionTemplate writeTransactionTemplate;
    private final TransactionTemplate readTransactionTemplate;

    private final JdbcTemplate writeTemplate;
    private final JdbcTemplate readTemplate;

    public TransactionService(PlatformTransactionManager writeTxManager,
            PlatformTransactionManager readTxManager, JdbcTemplate writeTemplate,
            JdbcTemplate readTemplate) {

        this.writeTransactionTemplate = new TransactionTemplate(writeTxManager);
        this.readTransactionTemplate = new TransactionTemplate(readTxManager);

        this.writeTemplate = writeTemplate;
        this.readTemplate = readTemplate;
    }

    // Transacción de escritura, con commit/rollback
    public <T> T write(Supplier<T> callback) {
        configureWriteTransaction(defaultConfig());
        return writeTransactionTemplate.execute(status -> callback.get());
    }

    public <T> T write(Supplier<T> callback, TransactionConfiguration configuration) {
        configureWriteTransaction(configuration);
        return writeTransactionTemplate.execute(status -> callback.get());
    }

    // Transacción de solo lectura, readOnly=true
    public <T> T read(Supplier<T> callback) {
        configureReadTransaction(defaultConfig());
        return readTransactionTemplate.execute(status -> callback.get());
    }

    public <T> T read(Supplier<T> callback, TransactionConfiguration configuration) {
        configureReadTransaction(configuration);
        return writeTransactionTemplate.execute(status -> callback.get());
    }

    private void configureWriteTransaction(TransactionConfiguration configuration) {
        if (configuration == null) {
            configuration = defaultConfig();
        }
        writeTransactionTemplate.setPropagationBehavior(configuration.getPropagation().value());
        writeTransactionTemplate.setIsolationLevel(configuration.getIsolation().value());
    }

    private void configureReadTransaction(TransactionConfiguration configuration) {
        if (configuration == null) {
            configuration = defaultConfig();
        }
        readTransactionTemplate.setPropagationBehavior(configuration.getPropagation().value());
        readTransactionTemplate.setIsolationLevel(configuration.getIsolation().value());
        readTransactionTemplate.setReadOnly(true);
    }

    private TransactionConfiguration defaultConfig() {
        return new TransactionConfiguration(Propagation.REQUIRED, Isolation.DEFAULT);
    }

}
