package io.easytx.routing;

import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

public class RoutingDataSource extends AbstractRoutingDataSource {

    private static final ThreadLocal<String> context = new ThreadLocal<>();

    public static void setDataSourceKey(String key) {
        context.set(key);
    }

    public static void clear() {
        context.remove();
    }

    @Override
    protected Object determineCurrentLookupKey() {
        return context.get();
    }

}
