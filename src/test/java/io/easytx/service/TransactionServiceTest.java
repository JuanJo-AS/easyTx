package io.easytx.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.IllegalTransactionStateException;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.transaction.annotation.Propagation;
import io.easytx.annotation.TransactionConfiguration;
import io.easytx.configuration.TestDataSourceConfig;

@SpringBootTest
@Import(TestDataSourceConfig.class)
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@ActiveProfiles("test")
class TransactionServiceTest {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private JdbcTemplate writeJdbcTemplate;

    @BeforeAll
    void setupSchema() {
        writeJdbcTemplate.execute("""
                    CREATE TABLE IF NOT EXISTS test_entity (
                        id SERIAL PRIMARY KEY,
                        name VARCHAR(100)
                    )
                """);
    }

    @BeforeEach
    void cleanTable() {
        writeJdbcTemplate.execute("DELETE FROM test_entity");
    }

    @Test
    void testWriteTransaction_commit() {
        transactionService.write(() -> {
            return insertIntoTestEntity("commit-test");
        });
        assertEquals(1, countFromTestEntity());
    }

    @Test
    void testWriteTransaction_rollback() {
        assertThrows(RuntimeException.class,
                () -> insertAndExceptionTransaction(null, "rollback-test", "Forcing rollback"));
        assertEquals(0, countFromTestEntity());
    }

    @Test
    void testIsolation_serializable() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRED, Isolation.SERIALIZABLE);

        transactionService.write(() -> {
            return insertIntoTestEntity("iso-test");
        }, config);
        assertEquals(1, countFromTestEntity());
    }

    @Test
    void testReadTransaction() {
        insertIntoTestEntity("read-test");
        List<String> result = transactionService.read(this::findNames);
        assertEquals(List.of("read-test"), result);
    }

    // PROPAGATION TESTS

    @Test
    void propagationRequired() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRED, Isolation.READ_COMMITTED);
        assertThrows(RuntimeException.class, () -> transactionService.write(() -> {
            insertIntoTestEntity("outer");
            insertTransaction(config, "inner");
            throw new RuntimeException("force rollback");
        }));
        assertEquals(0, countFromTestEntity()); // the rollback affected both transactions
    }

    /*
     * @Test void propagationRequiredWithAnotations() { assertThrows(RuntimeException.class, () ->
     * insertIntoTestEntityWithException("outer")); assertEquals(0, countFromTestEntity()); // the
     * rollback affected both transactions }
     */

    @Test()
    void propagationRequiresNew() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRES_NEW, Isolation.READ_COMMITTED);
        try {
            transactionService.write(() -> {
                insertIntoTestEntity("outer");
                insertTransaction(config, "inner");
                throw new RuntimeException("force rollback");
            });
        } catch (RuntimeException ignore) {
            // ignore
        }
        testEntityHasNames(List.of("inner")); // "outer" rollback, "inner" commit
    }

    @Test
    void propagationNested() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.NESTED, Isolation.READ_COMMITTED);
        transactionService.write(() -> {
            insertIntoTestEntity("outer");
            assertThrows(RuntimeException.class,
                    () -> insertAndExceptionTransaction(config, "inner", "rollback inner"));
            return null;
        });
        testEntityHasNames(List.of("outer"));
    }

    @Test
    void propagationSupportsWithinTransaction() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.SUPPORTS, Isolation.READ_COMMITTED);
        assertThrows(RuntimeException.class, () -> transactionService
                .write(() -> insertAndExceptionTransaction(config, "supports", "force rollback")));
        assertEquals(0, countFromTestEntity());
    }

    @Test
    void propagationSupportsWithoutTransaction() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.SUPPORTS, Isolation.READ_COMMITTED);
        assertThrows(RuntimeException.class,
                () -> insertAndExceptionTransaction(config, "supports", "force rollback"));
        testEntityHasNames(List.of("supports"));
    }

    @Test
    void propagationNotSupported() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.NOT_SUPPORTED, Isolation.READ_COMMITTED);
        assertThrows(RuntimeException.class, () -> {
            transactionService.write(() -> {
                insertIntoTestEntity("outer");
                assertThrows(RuntimeException.class,
                        () -> insertAndExceptionTransaction(config, "no-tx", "rollback inner"));
                throw new RuntimeException("rollback outer");
            });
        });
        testEntityHasNames(List.of("no-tx"));
    }

    @Test
    void propagationMandatory() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.MANDATORY, Isolation.READ_COMMITTED);
        // OK case (within a transaction)
        insertTransactionWithParentTransaction(config, "mandatory");
        // ERROR case (without a transaction)
        assertThrows(IllegalTransactionStateException.class,
                () -> insertTransaction(config, "should fail"));
        testEntityHasNames(List.of("mandatory"));
    }

    @Test
    void propagationNever() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.NEVER, Isolation.READ_COMMITTED);
        // ERROR case: within a transaction
        assertThrows(IllegalTransactionStateException.class,
                () -> insertTransactionWithParentTransaction(config, "never"));
        // OK case: outside of transaction
        insertTransaction(config, "outside");
        testEntityHasNames(List.of("outside"));
    }

    // TEST ISOLATION
    @Test
    void readUncommitedDirtyRead() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRES_NEW, Isolation.READ_UNCOMMITTED);
        Integer count = transactionService.write(() -> {
            insertIntoTestEntity("uncommitted");
            return countFromTestEntity();
        }, config);
        assertEquals(1, count); // can read the uncommitter insert
    }

    @Test
    void testIsolationReadCommitted() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRES_NEW, Isolation.READ_COMMITTED);
        Integer count = transactionService.write(() -> {
            insertIntoTestEntity("uncommitted");
            return transactionService.read(this::countFromTestEntity, config);
        }, config);
        assertEquals(0, count); // can't read the uncommitter insert
    }

    @Test
    void testIsolationSerializable() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRES_NEW, Isolation.SERIALIZABLE);
        transactionService.write(() -> {
            return insertIntoTestEntity("serializable-test");
        }, config);
        testEntityHasNames(List.of("serializable-test"));
    }

    @Test
    void testIsolation() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRES_NEW, Isolation.REPEATABLE_READ);
        insertIntoTestEntity("insert 1");
        transactionService.write(() -> {
            assertEquals(1, transactionService.read(this::countFromTestEntity, config));
            insertIntoTestEntity("insert 2");
            assertEquals(1, transactionService.read(this::countFromTestEntity, config));
            return null;
        }, config);
    }


    private int insertIntoTestEntityWithException(String name) throws RuntimeException {
        writeJdbcTemplate.update("INSERT INTO test_entity (name) VALUES (?)", name);
        insertIntoTestEntity("inner");
        throw new RuntimeException("force rollback");
    }

    private Integer insertTransactionWithParentTransaction(TransactionConfiguration config,
            String name) {
        return transactionService.write(() -> {
            return transactionService.write(() -> {
                return insertIntoTestEntity(name);
            }, config);
        });
    }

    private Integer insertTransaction(TransactionConfiguration config, String name) {
        return transactionService.write(() -> {
            return insertIntoTestEntity(name);
        }, config);
    }

    private Integer insertAndExceptionTransaction(TransactionConfiguration config, String name,
            String exception) {
        return transactionService.write(() -> {
            insertIntoTestEntity(name);
            throw new RuntimeException(exception);
        }, config);
    }

    private int insertIntoTestEntity(String name) {
        return writeJdbcTemplate.update("INSERT INTO test_entity (name) VALUES (?)", name);
    }

    private Integer countFromTestEntity() {
        return writeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_entity", Integer.class);
    }

    private List<String> findNames() {
        return writeJdbcTemplate.queryForList("SELECT name FROM test_entity", String.class);
    }

    private void testEntityHasNames(List<String> names) {
        assertEquals(names, findNames());
    }
}
