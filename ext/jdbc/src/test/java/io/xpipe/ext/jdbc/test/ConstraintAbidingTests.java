package io.xpipe.ext.jdbc.test;

import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.source.TableWriteConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.ext.jdbc.*;
import io.xpipe.ext.jdbc.source.JdbcTableSource;
import io.xpipe.ext.jdbc.source.JdbcWriteConnection;
import io.xpipe.ext.jdbc.test.item.AppendingInsertItem;
import io.xpipe.ext.jdbc.test.item.DatabaseItem;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.sql.Connection;

public class ConstraintAbidingTests extends DatabaseTest {

    @ParameterizedTest
    @MethodSource(value = "io.xpipe.ext.jdbc.test.item.DatabaseItem#getTableItems")
    public void testChainedOperations(DatabaseItem.TableItem item) throws Exception {
        System.setProperty(JdbcConfig.CLOSE_CONNECTIONS_PROPERTY, "false");
        var table = item.getTable();
        try (Connection connection = item.getStore().createConnection()) {
            connection.setCatalog(item.getDatabase());
            var type = JdbcDialect.getDialect(connection);
            var connectionStore = new JdbcConnectionStore(connection);
            var newTable = table + "_Test";

            try {
                JdbcHelper.execute(connection, type.createTableDropSql(newTable));
                JdbcHelper.execute(
                        connection,
                        type.createTableLikeSql(newTable, table, null, JdbcHelper.getPrimaryKeys(connection, table)));

                var inputSource = JdbcTableSource.builder()
                        .store(connectionStore)
                        .database(item.getDatabase())
                        .table(table)
                        .build();
                var outputSource = JdbcTableSource.builder()
                        .store(connectionStore)
                        .database(item.getDatabase())
                        .table(newTable)
                        .build();
                int size = 0;
                try (TableReadConnection tableReadConnection =
                                inputSource.openReadConnection().limit(50);
                        TableWriteConnection tableWriteConnection =
                                outputSource.openWriteConnection(WriteMode.REPLACE)) {
                    tableReadConnection.init();
                    tableWriteConnection.init();
                    size = tableReadConnection.forwardAndCount(tableWriteConnection);
                }
                System.out.println(String.format("Copied %s with %s lines", table, size));

                try (TableReadConnection tableReadConnection =
                                inputSource.openReadConnection().limit(50);
                        TableWriteConnection tableWriteConnection =
                                outputSource.openWriteConnection(WriteMode.REPLACE)) {
                    tableReadConnection.init();
                    tableWriteConnection.init();
                    size = tableReadConnection.forwardAndCount(tableWriteConnection);
                }

                try (TableReadConnection tableReadConnection = outputSource.openReadConnection()) {
                    tableReadConnection.init();
                    size = tableReadConnection.forwardAndCount(TableWriteConnection.empty());
                }

                ArrayNode original = null;
                try (TableReadConnection tableReadConnection =
                        inputSource.openReadConnection().limit(50)) {
                    tableReadConnection.init();
                    original = tableReadConnection.readRows(50);
                }

                ArrayNode copy = null;
                try (TableReadConnection tableReadConnection =
                        outputSource.openReadConnection().limit(50)) {
                    tableReadConnection.init();
                    copy = tableReadConnection.readRows(50);
                }

                assertNodeEquals(original, copy);
            } finally {
                JdbcHelper.execute(connection, type.createTableDropSql(newTable));
            }
        }
    }

    @ParameterizedTest
    @MethodSource(value = "io.xpipe.ext.jdbc.test.item.DatabaseItem#getTableItems")
    public void testSelfUpdate(DatabaseItem.TableItem item) throws Exception {
        var table = item.getTable();

        var inputSource = JdbcTableSource.builder()
                .store(item.getStore())
                .database(item.getDatabase())
                .table(table)
                .build();
        var outputSource = JdbcTableSource.builder()
                .store(item.getStore())
                .database(item.getDatabase())
                .table(table)
                .build();
        int size = 0;
        try (TableReadConnection tableReadConnection =
                inputSource.openReadConnection().limit(50)) {
            try (TableWriteConnection tableWriteConnection = outputSource.openWriteConnection(JdbcWriteModes.MERGE)) {
                tableReadConnection.init();
                tableWriteConnection.init();
                ((JdbcWriteConnection) tableWriteConnection).persistent(false);
                size = tableReadConnection.forwardAndCount(tableWriteConnection);
            }
        }
        System.out.println(String.format("Finished %s with %s lines", table, size));
    }

    @ParameterizedTest
    @MethodSource(value = "io.xpipe.ext.jdbc.test.item.AppendingInsertItem#getAll")
    public void testAppendingInsert(AppendingInsertItem item) throws Exception {
        try (Connection connection = item.getTableItem().getStore().createConnection()) {
            connection.setCatalog(item.getTableItem().getDatabase());
            var connectionStore = new JdbcConnectionStore(connection);
            var table = item.getTableItem().getTable();

            var outputSource = JdbcTableSource.builder()
                    .store(connectionStore)
                    .database(item.getTableItem().getDatabase())
                    .table(table)
                    .build();
            var outputType = item.getInsert().determineDataType().asTuple();
            var mapping = outputSource.createMapping(outputType).orElseThrow();
            try (TableWriteConnection tableWriteConnection = outputSource.openWriteConnection(JdbcWriteModes.MERGE)) {
                ((JdbcWriteConnection) tableWriteConnection).persistent(false);
                tableWriteConnection.writeLinesAcceptor(mapping).accept(item.getInsert());
            }
        }
    }
}
