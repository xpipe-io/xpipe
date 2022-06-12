package io.xpipe.core.source;

import io.xpipe.core.data.node.*;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.data.type.ValueType;
import io.xpipe.core.store.JdbcStore;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collections;

@Data
@EqualsAndHashCode(callSuper = true)
public abstract class JdbcQuerySource extends TableDataSource<JdbcStore> {

    JdbcStore store;

    protected abstract String createQuery();

    public Connection createConnection() throws SQLException {
        return store.createConnection();
    }

    @Override
    protected boolean supportsRead() {
        return true;
    }

    @Override
    protected TableReadConnection newReadConnection() {
        return new TableReadConnection() {

            private Connection connection;
            private Statement statement;
            private TupleType dataType;
            private ResultSet resultSet;

            @Override
            public void init() throws Exception {
                connection = createConnection();
                statement = connection.createStatement();

                resultSet = statement.executeQuery(createQuery());
                var meta = resultSet.getMetaData();
                var names = new ArrayList<String>();
                for (int i = 0; i < meta.getColumnCount(); i++) {
                    names.add(meta.getColumnName(i + 1));
                }
                dataType = TupleType.of(names, Collections.nCopies(names.size(), ValueType.of()));
            }

            @Override
            public void close() throws Exception {
                statement.close();
                connection.close();
            }

            @Override
            public TupleType getDataType() {
                return dataType;
            }

            @Override
            public int getRowCount() throws Exception {
                return resultSet.getFetchSize();
            }

            @Override
            public void withRows(DataStructureNodeAcceptor<TupleNode> lineAcceptor) throws Exception {
                while (resultSet.next()) {
                    var vals = new ArrayList<DataStructureNode>();
                    for (int i = 0; i < dataType.getSize(); i++) {
                        vals.add(ValueNode.of(resultSet.getString(i)));
                    }

                    var node = TupleNode.of(dataType.getNames(), vals);
                    if (!lineAcceptor.accept(node)) {
                        break;
                    }
                }
            }

            @Override
            public ArrayNode readRows(int maxLines) throws Exception {
                return null;
            }
        };
    }
}
