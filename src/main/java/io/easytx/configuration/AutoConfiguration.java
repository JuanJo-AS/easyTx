package io.easytx.configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import javax.sql.DataSource;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import io.easytx.routing.RoutingDataSource;
import io.easytx.service.TransactionService;

@Configuration
public class AutoConfiguration {

    private static final String WRITE = "write";
    private static final String READ = "read";
    private static final String TX_WRITE_SOURCE = "txWriteSource";
    private static final String TX_READ_SOURCE = "txReadSource";
    private static final String WRITE_TX_MANAGER = "writeTxManager";
    private static final String READ_TX_MANAGER = "readTxManager";
    private static final String WRITE_JDBC_TEMPLATE = "writeJdbcTemplate";
    private static final String READ_JDBC_TEMPLATE = "readJdbcTemplate";
    private static final String DS_OR_ROUTING_AVAILABLE =
            " DataSource or routingDataSource available";

    @Bean
    @ConditionalOnMissingBean(name = WRITE_TX_MANAGER)
    public PlatformTransactionManager writeTxManager(
            @Autowired(required = false) @Qualifier(TX_WRITE_SOURCE) DataSource txWriteSource,
            @Autowired(required = false) DataSource routingDataSource) {
        return configureTxManager(txWriteSource, routingDataSource, WRITE);
    }

    @Bean
    @ConditionalOnMissingBean(name = READ_TX_MANAGER)
    public PlatformTransactionManager readTxManager(
            @Autowired(required = false) @Qualifier(TX_READ_SOURCE) DataSource txReadSource,
            @Autowired(required = false) DataSource routingDataSource) {
        return configureTxManager(txReadSource, routingDataSource, READ);
    }

    private PlatformTransactionManager configureTxManager(DataSource dataSource,
            DataSource routingDataSource, String type) {
        if (dataSource != null) {
            return new DataSourceTransactionManager(dataSource);
        } else if (routingDataSource != null) {
            return new DataSourceTransactionManager(getFromRouting(routingDataSource, type));
        } else {
            throw new IllegalStateException("No " + type + DS_OR_ROUTING_AVAILABLE);
        }
    }

    @Bean
    @ConditionalOnMissingBean(name = WRITE_JDBC_TEMPLATE)
    public JdbcTemplate writeJdbcTemplate(
            @Autowired(required = false) @Qualifier(TX_WRITE_SOURCE) DataSource txWriteSource,
            @Autowired(required = false) DataSource routingDataSource) {
        return configureTemplate(txWriteSource, routingDataSource, WRITE);
    }

    @Bean
    @ConditionalOnMissingBean(name = READ_JDBC_TEMPLATE)
    public JdbcTemplate readJdbcTemplate(
            @Autowired(required = false) @Qualifier(TX_READ_SOURCE) DataSource txReadSource,
            @Autowired(required = false) DataSource routingDataSource) {
        return configureTemplate(txReadSource, routingDataSource, READ);
    }

    private JdbcTemplate configureTemplate(DataSource txReadSource, DataSource routingDataSource,
            String type) {
        if (txReadSource != null) {
            return new JdbcTemplate(txReadSource);
        } else if (routingDataSource != null) {
            return new JdbcTemplate(getFromRouting(routingDataSource, type));
        } else {
            throw new IllegalStateException("No " + type + DS_OR_ROUTING_AVAILABLE);
        }
    }

    private DataSource getFromRouting(DataSource routingDataSource, String type) {
        return ((RoutingDataSource) routingDataSource).getResolvedDataSources().get(type);
    }

    @Bean
    public TransactionService transactionService(
            @Qualifier(WRITE_TX_MANAGER) PlatformTransactionManager writeTxManager,
            @Qualifier(READ_TX_MANAGER) PlatformTransactionManager readTxManager,
            @Qualifier(WRITE_JDBC_TEMPLATE) JdbcTemplate writeJdbcTemplate,
            @Qualifier(READ_JDBC_TEMPLATE) JdbcTemplate readJdbcTemplate) {
        return new TransactionService(writeTxManager, readTxManager, writeJdbcTemplate,
                readJdbcTemplate);
    }

    @Bean
    public DataSource routingDataSource(ObjectProvider<Map<String, DataSource>> txSourcesProvider,
            ObjectProvider<DataSource> writeDataSourceProvider,
            ObjectProvider<DataSource> readDataSourceProvider) {
        Map<Object, Object> finalSources = validateAndCreateDataSources(txSourcesProvider,
                writeDataSourceProvider, readDataSourceProvider);
        RoutingDataSource routing = new RoutingDataSource();
        routing.setTargetDataSources(finalSources);
        routing.setDefaultTargetDataSource(finalSources.get(WRITE));
        return routing;
    }

    public Map<Object, Object> validateAndCreateDataSources(
            ObjectProvider<Map<String, DataSource>> txSourcesProvider,
            ObjectProvider<DataSource> writeDataSourceProvider,
            ObjectProvider<DataSource> readDataSourceProvider) {
        Map<String, DataSource> userDefined = txSourcesProvider.getIfAvailable();
        DataSource txWriteSource = writeDataSourceProvider.getIfAvailable();
        DataSource txReadSource = readDataSourceProvider.getIfAvailable();

        Map<Object, Object> finalSources = new HashMap<>();
        if (userDefined != null && !userDefined.isEmpty()) {
            finalSources.putAll(userDefined);
        }
        if (txWriteSource != null) {
            finalSources.put(WRITE, txWriteSource);
        }
        if (txReadSource != null) {
            finalSources.put(READ, txReadSource);
        }
        validateSources(finalSources);
        return finalSources;
    }

    @Bean
    @ConditionalOnMissingBean(name = "transactionManager")
    public DataSourceTransactionManager transactionManager(DataSource routingDataSource) {
        return new DataSourceTransactionManager(routingDataSource);
    }

    private void validateSources(Map<Object, Object> sources) {
        if (mainKeysNotValid(sources)) {
            throw new IllegalArgumentException(
                    "EasyTx configuration error: final DataSource map must contain non-null entries for keys 'write' and 'read'. "
                            + "Provide them either in the 'txSources' map (keys 'write' and 'read') or as beans 'txWriteSource' and 'txReadSource'.");
        }
    }

    private boolean mainKeysNotValid(Map<Object, Object> sources) {
        return missingMainKeys(sources.keySet())
                || (sources.get(WRITE) == null || sources.get(READ) == null);
    }

    private boolean missingMainKeys(Set<Object> keys) {
        return !keys.contains(WRITE) || !keys.contains(READ);
    }
}
