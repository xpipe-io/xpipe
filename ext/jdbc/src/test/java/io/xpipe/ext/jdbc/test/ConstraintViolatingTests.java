package io.xpipe.ext.jdbc.test;

import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.source.TableWriteConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.ext.jdbc.JdbcConnectionStore;
import io.xpipe.ext.jdbc.JdbcDialect;
import io.xpipe.ext.jdbc.source.JdbcTableSource;
import io.xpipe.ext.jdbc.source.JdbcWriteConnection;
import io.xpipe.ext.jdbc.test.item.DatabaseItem;
import io.xpipe.ext.jdbc.test.item.InsertItem;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;
import java.sql.SQLException;

@Disabled
public class ConstraintViolatingTests extends DatabaseTest {

    @BeforeEach
    public void prepareDatabases() throws SQLException {
        for (DatabaseItem item : DatabaseItem.getAll().map(databaseItemNamed -> databaseItemNamed.getPayload()).toList()) {
            try (Connection connection = item.getStore().createConnection()) {
                connection.setCatalog(item.getDatabase());
                var type = JdbcDialect.getDialect(connection);
                type.disableConstraints(connection);
            }
        }
    }

    @ParameterizedTest
    @MethodSource(value = "io.xpipe.ext.jdbc.test.item.InsertItem#getAll")
    public void testReplacingInsert(InsertItem item) throws Exception {
        try (Connection connection = item.tableItem.getStore().createConnection()) {
            connection.setCatalog(item.tableItem.getDatabase());
            var connectionStore = new JdbcConnectionStore(connection);
            var table = item.tableItem.getTable();

            System.out.println(String.format("Starting %s", table));
            var outputSource = JdbcTableSource.builder()
                    .table(table)
                    .store(connectionStore)
                    .database(item.tableItem.getDatabase())
                    .build();
            var outputType = item.insert.determineDataType().asTuple();
            var mapping = outputSource.createMapping(outputType).orElseThrow();
            try (TableWriteConnection tableWriteConnection = outputSource.openWriteConnection(WriteMode.REPLACE)) {
                ((JdbcWriteConnection) tableWriteConnection).persistent(false);
                tableWriteConnection.writeLinesAcceptor(mapping).accept(item.insert);
            }
            System.out.println(String.format("Finished %s", table));
        }
    }

    @ParameterizedTest
    @MethodSource(value = "io.xpipe.ext.jdbc.test.item.DatabaseItem#getDisabledConstraintTableItems")
    public void testSelfReplace(DatabaseItem.TableItem item) throws Exception {
        try (Connection connection = item.getStore().createConnection()) {
            connection.setCatalog(item.getDatabase());
            var connectionStore = new JdbcConnectionStore(connection);
            var table = item.getTable();

            var inputSource = JdbcTableSource.builder()
                    .table(table)
                    .store(connectionStore)
                    .database(item.getDatabase())
                    .build();
            var outputSource = JdbcTableSource.builder()
                    .table(table)
                    .store(connectionStore)
                    .database(item.getDatabase())
                    .build();
            int size = 0;
            try (TableReadConnection tableReadConnection =
                    inputSource.openReadConnection().limit(50)) {
                try (TableWriteConnection tableWriteConnection = outputSource.openWriteConnection(WriteMode.REPLACE)) {
                    tableReadConnection.init();
                    tableWriteConnection.init();
                    ((JdbcWriteConnection) tableWriteConnection).persistent(false);
                    size = tableReadConnection.forwardAndCount(tableWriteConnection);
                }
            }
        }
    }
}
