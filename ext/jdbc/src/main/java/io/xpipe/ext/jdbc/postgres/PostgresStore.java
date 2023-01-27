package io.xpipe.ext.jdbc.postgres;

import io.xpipe.ext.jdbc.JdbcBaseStore;

import java.util.Map;

public interface PostgresStore extends JdbcBaseStore {

    @Override
    default Map<String, Object> createDefaultProperties() {
        return Map.of("ApplicationName.", "X-Pipe");
    }
}
