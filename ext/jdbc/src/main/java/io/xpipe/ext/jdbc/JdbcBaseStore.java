package io.xpipe.ext.jdbc;

import io.xpipe.core.util.Proxyable;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public interface JdbcBaseStore extends JdbcStore, Proxyable {

    @Override
    default Connection createConnection() throws SQLException {
        var baseProperties = new HashMap<>(createDefaultProperties());
        baseProperties.putAll(createProperties());

        Properties properties = new Properties();
        properties.putAll(baseProperties);

        return DriverManager.getConnection(toUrl(), properties);
    }

    String toUrl();

    Map<String, String> createProperties();

    default Map<String, Object> createDefaultProperties() {
        return Map.of();
    }
}
