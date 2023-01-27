package io.xpipe.ext.jdbc;

import io.xpipe.core.store.DataStore;

import java.sql.Connection;
import java.sql.SQLException;

public interface JdbcStore extends DataStore {

    @Override
    default void validate() throws Exception {
        try (Connection con = createConnection()) {
            con.getMetaData().getCatalogs();
        }
    }

    Connection createConnection() throws SQLException;
}
