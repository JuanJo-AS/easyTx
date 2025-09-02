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
        transactionService.withWriteTransaction(() -> {
            writeJdbcTemplate.update("INSERT INTO test_entity (name) VALUES (?)", "commit-test");
            return null;
        });

        Integer count =
                writeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_entity", Integer.class);
        assertEquals(1, count);
    }

    @Test
    void testWriteTransaction_rollback() {
        assertThrows(RuntimeException.class, () -> transactionService.withWriteTransaction(() -> {
            writeJdbcTemplate.update("INSERT INTO test_entity (name) VALUES (?)", "rollback-test");
            throw new RuntimeException("Forcing rollback");
        }));

        Integer count =
                writeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_entity", Integer.class);
        assertEquals(0, count);
    }

    @Test
    void testPropagation_requiresNew() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRES_NEW, Isolation.READ_COMMITTED);

        transactionService.withWriteTransaction(() -> {
            writeJdbcTemplate.update("INSERT INTO test_entity (name) VALUES (?)", "outer");
            transactionService.withWriteTransaction(() -> {
                writeJdbcTemplate.update("INSERT INTO test_entity (name) VALUES (?)", "inner");
                return null;
            }, config);
            return null;
        });

        Integer count =
                writeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_entity", Integer.class);
        assertEquals(2, count);
    }

    @Test
    void testIsolation_serializable() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRED, Isolation.SERIALIZABLE);

        transactionService.withWriteTransaction(() -> {
            writeJdbcTemplate.update("INSERT INTO test_entity (name) VALUES (?)", "iso-test");
            return null;
        }, config);

        Integer count =
                writeJdbcTemplate.queryForObject("SELECT COUNT(*) FROM test_entity", Integer.class);
        assertEquals(1, count);
    }

    @Test
    void testReadTransaction() {
        writeJdbcTemplate.update("INSERT INTO test_entity (name) VALUES (?)", "read-test");

        String result = transactionService.withReadTransaction(() -> writeJdbcTemplate
                .queryForObject("SELECT name FROM test_entity LIMIT 1", String.class));

        assertEquals("read-test", result);
    }

    @Test
    void testPropagationRequired() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRED, Isolation.READ_COMMITTED);
        assertThrows(RuntimeException.class, () -> transactionService.withWriteTransaction(() -> {
            insertIntoTestEntity("outer");
            transactionService.withWriteTransaction(() -> {
                return insertIntoTestEntity("inner");
            }, config);
            throw new RuntimeException("force rollback");
        }));
        assertEquals(0, countFromTestEntity()); // the rollback affected both transactions
    }

    @Test
    void testPropagationRequiredWithAnotations() {
        assertThrows(RuntimeException.class, () -> insertIntoTestEntityWithException("outer"));
        assertEquals(0, countFromTestEntity()); // the rollback affected both transactions
    }

    @Test()
    void testPropagationRequiresNew() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRES_NEW, Isolation.READ_COMMITTED);
        try {
            transactionService.withWriteTransaction(() -> {
                insertIntoTestEntity("outer");
                transactionService.withWriteTransaction(() -> {
                    return insertIntoTestEntity("inner");
                }, config);
                throw new RuntimeException("force rollback");
            });
        } catch (RuntimeException ignore) {
        }
        assertEquals(List.of("inner"), findNames()); // "outer" rollback, "inner" commit
    }

    @Test
    void testPropagationNested() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.NESTED, Isolation.READ_COMMITTED);

        transactionService.withWriteTransaction(() -> {
            insertIntoTestEntity("outer");
            assertThrows(RuntimeException.class,
                    () -> transactionService.withWriteTransaction(() -> {
                        insertIntoTestEntity("inner");
                        throw new RuntimeException("rollback inner");
                    }, config));
            return null;
        });
        assertEquals(List.of("outer"), findNames());
    }

    @Test
    void testIsolationReadCommitted() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRES_NEW, Isolation.READ_COMMITTED);

        transactionService.withWriteTransaction(() -> {
            insertIntoTestEntity("uncommitted");

            // en paralelo simulamos otra transacción REQUIRES_NEW con READ_COMMITTED
            String result = transactionService
                    .withWriteTransaction(this::countFromTestEntity, config).toString();

            assertEquals("0", result); // no ve el insert sin commit
            return null;
        });
    }

    @Test
    void testIsolationSerializable() {
        TransactionConfiguration config =
                new TransactionConfiguration(Propagation.REQUIRES_NEW, Isolation.SERIALIZABLE);

        transactionService.withWriteTransaction(() -> {
            return insertIntoTestEntity("serializable-test");
        }, config);

        assertEquals(1, countFromTestEntity());
    }


    private int insertIntoTestEntityWithException(String name) throws RuntimeException {
        writeJdbcTemplate.update("INSERT INTO test_entity (name) VALUES (?)", name);
        insertIntoTestEntity("inner");
        throw new RuntimeException("force rollback");
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
}
