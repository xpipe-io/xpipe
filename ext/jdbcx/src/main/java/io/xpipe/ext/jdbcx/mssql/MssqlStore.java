package io.xpipe.ext.jdbcx.mssql;

import io.xpipe.ext.jdbc.JdbcBaseStore;

import java.util.Map;

public interface MssqlStore extends JdbcBaseStore {

    @Override
    default Map<String, Object> createDefaultProperties() {
        return Map.of("applicationName", "X-Pipe", "loginTimeout", "5");
    }
}
