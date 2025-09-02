package io.easytx.configuration;

import javax.sql.DataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@TestConfiguration
@Profile("test")
public class TestDataSourceConfig {

    @Bean(name = "writeDataSource")
    @Primary
    public DataSource writeDataSource() {
        return DataSourceBuilder.create().driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:writeDb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL").username("sa")
                .password("").build();
    }

    @Bean(name = "readDataSource")
    public DataSource readDataSource() {
        return DataSourceBuilder.create().driverClassName("org.h2.Driver")
                .url("jdbc:h2:mem:readDb;DB_CLOSE_DELAY=-1;MODE=PostgreSQL").username("sa")
                .password("").build();
    }

    @Bean(name = "writeTransactionManager")
    public PlatformTransactionManager writeTransactionManager(
            @Qualifier("writeDataSource") DataSource writeDataSource) {
        return new DataSourceTransactionManager(writeDataSource);
    }

    @Bean(name = "readTransactionManager")
    public PlatformTransactionManager readTransactionManager(
            @Qualifier("readDataSource") DataSource readDataSource) {
        return new DataSourceTransactionManager(readDataSource);
    }

    @Bean(name = "writeJdbcTemplate")
    public JdbcTemplate writeJdbcTemplate(
            @Qualifier("writeDataSource") DataSource writeDataSource) {
        return new JdbcTemplate(writeDataSource);
    }

    @Bean(name = "readJdbcTemplate")
    public JdbcTemplate readJdbcTemplate(@Qualifier("readDataSource") DataSource readDataSource) {
        return new JdbcTemplate(readDataSource);
    }
}
