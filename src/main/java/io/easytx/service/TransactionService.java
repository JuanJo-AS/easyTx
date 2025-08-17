package io.easytx.service;

import java.util.List;
import java.util.function.Supplier;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import io.easytx.annotation.LogLevel;
import io.easytx.annotation.Read;
import io.easytx.annotation.TransactionConfiguration;
import io.easytx.annotation.TxRead;
import io.easytx.annotation.TxWrite;
import io.easytx.annotation.Write;

@Service
public class TransactionService {

    private final TransactionTemplate writeTransactionTemplate;
    private final TransactionTemplate readTransactionTemplate;

    private final JdbcTemplate writeJdbcTemplate;
    private final JdbcTemplate readJdbcTemplate;

    public TransactionService(
            @Qualifier("writeTransactionManager") PlatformTransactionManager writeTxManager,
            @Qualifier("readTransactionManager") PlatformTransactionManager readTxManager,
            @Qualifier("writeDataSource") javax.sql.DataSource writeDataSource,
            @Qualifier("readDataSource") javax.sql.DataSource readDataSource) {

        this.writeTransactionTemplate = new TransactionTemplate(writeTxManager);
        this.readTransactionTemplate = new TransactionTemplate(readTxManager);

        this.writeJdbcTemplate = new JdbcTemplate(writeDataSource);
        this.readJdbcTemplate = new JdbcTemplate(readDataSource);
    }

    // Transacción de escritura, con commit/rollback
    public <T> T withWriteTransaction(Supplier<T> callback) {
        return writeTransactionTemplate.execute(status -> callback.get());
    }

    public <T> T withWriteTransaction(Supplier<T> callback,
            TransactionConfiguration configuration) {
        writeTransactionTemplate.setPropagationBehavior(configuration.getPropagation().value());
        writeTransactionTemplate.setIsolationLevel(configuration.getIsolation().value());
        return writeTransactionTemplate.execute(status -> callback.get());
    }

    // Transacción de solo lectura, readOnly=true
    public <T> T withReadTransaction(Supplier<T> callback) {
        readTransactionTemplate.setReadOnly(true);
        return readTransactionTemplate.execute(status -> callback.get());
    }

    public <T> T withReadTransaction(Supplier<T> callback, TransactionConfiguration configuration) {
        readTransactionTemplate.setPropagationBehavior(configuration.getPropagation().value());
        readTransactionTemplate.setIsolationLevel(configuration.getIsolation().value());
        return writeTransactionTemplate.execute(status -> callback.get());
    }

    // Ejemplo de acceso a jdbc para lectura (puedes extenderlo)
    @TxRead(logLevel = LogLevel.ALL)
    public List<String> readTransactionExample() {
        return withReadTransaction(() -> readJdbcTemplate.query("SELECT mensaje FROM test_txlens",
                (rs, rowNum) -> rs.getString("mensaje")));
    }

    // Ejemplo de acceso a jdbc para escritura
    @TxWrite()
    public int writeTransactionExample(String message) {
        return withWriteTransaction(() -> writeJdbcTemplate
                .update("INSERT INTO test_txlens (mensaje) VALUES (?)", message));
    }

    @Read
    public List<String> readExample() {
        return readJdbcTemplate.query("SELECT mensaje FROM test_txlens",
                (rs, rowNum) -> rs.getString("mensaje"));
    }

    @Write
    public int writeExample(String message) {
        return writeJdbcTemplate.update("INSERT INTO test_txlens (mensaje) VALUES (?)", message);
    }
}
