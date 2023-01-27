package io.xpipe.ext.jdbc.source;

import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.DataFlow;
import lombok.experimental.SuperBuilder;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;

@SuperBuilder
public abstract class JdbcQuerySource extends JdbcSource {

    @Override
    public List<WriteMode> getAvailableWriteModes() {
        return List.of();
    }

    @Override
    public DataFlow getFlow() {
        return DataFlow.INPUT;
    }

    protected abstract String createQuery() throws Exception;

    @Override
    protected TableReadConnection newReadConnection() {
        return new JdbcReadConnection() {
            @Override
            public boolean canRead() throws Exception {
                return true;
            }

            @Override
            protected PreparedStatement createQueryStatement() throws Exception {
                var query = createQuery();
                return connection.prepareStatement(query);
            }

            @Override
            protected Connection createConnection() throws SQLException {
                var connection = JdbcQuerySource.this.createConnection();
                connection.setCatalog(database);
                return connection;
            }
        };
    }
}
