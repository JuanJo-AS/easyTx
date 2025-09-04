package io.easytx.configuration;

import java.util.Map;
import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;
import io.easytx.routing.RoutingDataSource;
import io.easytx.service.TransactionService;

@Configuration
public class AutoConfiguration {

    @Bean
    public PlatformTransactionManager writeTransactionManager(
            @Qualifier("writeDataSource") DataSource writeDataSource) {
        return new DataSourceTransactionManager(writeDataSource);
    }

    @Bean
    public PlatformTransactionManager readTransactionManager(
            @Qualifier("readDataSource") DataSource readDataSource) {
        return new DataSourceTransactionManager(readDataSource);
    }

    @Bean
    public JdbcTemplate writeJdbcTemplate(
            @Qualifier("writeDataSource") DataSource writeDataSource) {
        return new JdbcTemplate(writeDataSource);
    }

    @Bean
    public JdbcTemplate readJdbcTemplate(@Qualifier("readDataSource") DataSource readDataSource) {
        return new JdbcTemplate(readDataSource);
    }

    @Bean
    public TransactionService transactionService(
            @Qualifier("writeTransactionManager") PlatformTransactionManager writeTxManager,
            @Qualifier("readTransactionManager") PlatformTransactionManager readTxManager,
            @Qualifier("writeDataSource") DataSource writeDataSource,
            @Qualifier("readDataSource") DataSource readDataSource) {
        return new TransactionService(writeTxManager, readTxManager, writeDataSource,
                readDataSource);
    }

    @Bean
    public DataSource routingDataSource(@Qualifier("dataSources") Map<Object, Object> dataSources,
            @Qualifier("writeDataSource") DataSource writeDataSource,
            @Qualifier("readDataSource") DataSource readDataSource) {

        if (!dataSources.containsKey("write")) {
            dataSources.put("write", writeDataSource);
        }
        if (!dataSources.containsKey("read")) {
            dataSources.put("read", readDataSource);
        }

        RoutingDataSource routing = new RoutingDataSource();
        routing.setTargetDataSources(dataSources);
        routing.setDefaultTargetDataSource(writeDataSource);
        return routing;
    }

    @Bean
    public DataSourceTransactionManager transactionManager(DataSource routingDataSource) {
        return new DataSourceTransactionManager(routingDataSource);
    }
}
