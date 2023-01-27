package io.xpipe.ext.jdbc.source;

import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.DataStructureNodeAcceptor;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.ValueType;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.ext.jdbc.JdbcConfig;
import io.xpipe.ext.jdbc.JdbcDialect;
import io.xpipe.ext.jdbc.JdbcHelper;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Collections;

public abstract class JdbcReadConnection implements TableReadConnection {

    protected Connection connection;
    protected TupleType dataType;
    protected JdbcDialect type;
    private boolean init;
    private PreparedStatement statement;
    private ResultSet resultSet;

    protected abstract PreparedStatement createQueryStatement() throws Exception;

    protected abstract Connection createConnection() throws SQLException;

    @Override
    public void init() throws Exception {
        connection = createConnection();
        type = JdbcDialect.getDialect(connection);

        statement = createQueryStatement();
        if (statement == null) {
            return;
        }

        resultSet = JdbcHelper.executeQueryStatement(statement);
        var meta = resultSet.getMetaData();
        var names = new ArrayList<String>();
        for (int i = 0; i < meta.getColumnCount(); i++) {
            names.add(meta.getColumnName(i + 1));
        }
        dataType = TupleType.of(names, Collections.nCopies(names.size(), ValueType.of()));

        init = true;
    }

    @Override
    public void close() throws Exception {
        if (resultSet != null) {
            resultSet.close();
        }

        if (statement != null) {
            statement.close();
        }

        if (connection != null) {
            if (JdbcConfig.closeConnections()) {
                connection.close();
            }
        }
    }

    @Override
    public TupleType getDataType() {
        return dataType;
    }

    @Override
    public void withRows(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception {
        if (!init) {
            return;
        }

        while (resultSet.next()) {
            var types = JdbcHelper.getColumnTypes(resultSet);
            var vals = new ArrayList<DataStructureNode>();
            for (int i = 0; i < dataType.getSize(); i++) {
                var columnIndex = i + 1;
                var dataType = types.get(i);
                var category = type.getCategory(resultSet, i);
                var value = category.getValue(resultSet, dataType, columnIndex);
                vals.add(value);
            }

            var node = TupleNode.of(dataType.getNames(), vals);
            if (!lineAcceptor.accept(node)) {
                break;
            }
        }
    }
}
