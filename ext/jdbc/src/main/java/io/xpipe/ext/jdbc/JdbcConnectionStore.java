package io.xpipe.ext.jdbc;

import com.fasterxml.jackson.annotation.JsonIgnore;
import io.xpipe.core.util.JacksonizedValue;
import lombok.AllArgsConstructor;

import java.sql.Connection;
import java.sql.SQLException;

@AllArgsConstructor
public class JdbcConnectionStore extends JacksonizedValue implements JdbcStore {

    @JsonIgnore
    Connection connection;

    @Override
    public Connection createConnection() throws SQLException {
        return connection;
    }
}
