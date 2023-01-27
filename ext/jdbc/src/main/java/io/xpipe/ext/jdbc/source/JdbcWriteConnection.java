package io.xpipe.ext.jdbc.source;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.impl.BatchTableWriteConnection;
import io.xpipe.core.source.TableMapping;
import io.xpipe.core.source.WriteMode;
import io.xpipe.ext.jdbc.JdbcConfig;
import io.xpipe.ext.jdbc.JdbcDialect;
import io.xpipe.ext.jdbc.JdbcHelper;
import io.xpipe.extension.event.TrackEvent;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public abstract class JdbcWriteConnection extends BatchTableWriteConnection {

    protected final WriteMode mode;
    private final JdbcSource source;
    protected Connection connection;
    protected JdbcDialect type;
    private boolean persistent = true;

    protected JdbcWriteConnection(JdbcSource source, WriteMode mode) {
        this.source = source;
        this.mode = mode;
    }

    public JdbcWriteConnection persistent(boolean persistent) {
        this.persistent = persistent;
        return this;
    }

    @Override
    protected DataStructureNodeAcceptor<ArrayNode> writeBatchLinesAcceptor(TableMapping mapping) {
        return node -> {
            var statement = createInsertStatement(mapping, node);
            if (node.size() > 1) {
                JdbcHelper.executeBatchStatement(statement);
            } else {
                JdbcHelper.executeStatement(statement);
            }
            return true;
        };
    }

    @Override
    public void init() throws Exception {
        connection = source.createConnection();
        type = JdbcDialect.getDialect(connection);
        connection.setAutoCommit(false);
        source.prepareConnection(connection);
    }

    protected abstract PreparedStatement createInsertStatement(TableMapping mapping, ArrayNode node)
            throws SQLException;

    @Override
    public void onClose(TableMapping mapping) throws Exception {
        if (connection != null) {
            if (persistent) {
                TrackEvent.trace("jdbc", "Committing transaction");
                connection.commit();
            } else {
                TrackEvent.trace("jdbc", "Rolling back transaction as changes are not persistent");
                connection.rollback();
            }

            if (JdbcConfig.closeConnections()) {
                TrackEvent.trace("jdbc", "Closing connection " + connection.getClientInfo());
                connection.close();
                connection = null;
            }
        }
    }
}
