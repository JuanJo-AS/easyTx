package io.easytx.configuration;

import javax.sql.DataSource;

// TODO: allow users to configure their own dataSources
public interface DataSourceProvider {

    DataSource getReadDataSource();

    DataSource getWriteDataSource();
}
