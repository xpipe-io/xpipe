package io.xpipe.ext.jdbc.source;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.data.node.ArrayNode;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.type.TupleType;
import io.xpipe.core.source.TableMapping;
import io.xpipe.core.source.TableReadConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.DataFlow;
import io.xpipe.ext.jdbc.JdbcHelper;
import io.xpipe.ext.jdbc.JdbcWriteModes;
import io.xpipe.extension.util.Validators;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

@SuperBuilder
@Jacksonized
@Getter
@JsonTypeName("jdbcTable")
public class JdbcTableSource extends JdbcQuerySource {

    private final String table;

    @Override
    public Optional<String> determineDefaultName() {
        return Optional.ofNullable(table);
    }

    @Override
    public List<WriteMode> getAvailableWriteModes() {
        return List.of(WriteMode.REPLACE, JdbcWriteModes.UPDATE, JdbcWriteModes.INSERT, JdbcWriteModes.MERGE);
    }

    @Override
    public DataFlow getFlow() {
        return DataFlow.INPUT_OUTPUT;
    }

    @Override
    public void checkComplete() throws Exception {
        super.checkComplete();
        Validators.nonNull(table, "Table");
    }

    @Override
    protected String createQuery() {
        if (table == null) {
            return null;
        }

        return "SELECT * from " + table;
    }

    @Override
    protected TableReadConnection newReadConnection() {
        return new JdbcReadConnection() {

            @Override
            public boolean canRead() throws Exception {
                return true;
            }

            @Override
            protected PreparedStatement createQueryStatement() throws SQLException {
                var map = JdbcTableParameterMap.ColumnInformation.create(connection, table);
                var order = new ArrayList<>(map.getIdentifiers());
                if (order.size() == 0) {
                    order.add(map.getAllTableColumns().get(0));
                }

                return type.createTableSelectStatement(connection, table, order);
            }

            @Override
            protected Connection createConnection() throws SQLException {
                var connection = store.createConnection();
                connection.setCatalog(database);
                return connection;
            }
        };
    }

    @Override
    protected JdbcWriteConnection newWriteConnection(WriteMode mode) {
        if (mode.equals(WriteMode.REPLACE) || mode.equals(JdbcWriteModes.MERGE)) {
            return new WriteConnection(this, mode);
        }

        throw new UnsupportedOperationException(mode.getId());
    }

    public class WriteConnection extends JdbcWriteConnection {
        private String targetTable;
        private boolean updatePrepared;

        protected WriteConnection(JdbcSource source, WriteMode append) {
            super(source, append);
        }

        @Override
        public Optional<TableMapping> createMapping(TupleType inputType) throws SQLException {
            return JdbcTableParameterMap.create(connection, table, inputType, mode)
                    .map(jdbcTableParameterMap -> jdbcTableParameterMap);
        }

        private void prepareTableUpdate(JdbcTableParameterMap mapping) throws SQLException {
            if (updatePrepared) {
                return;
            }

            if (mapping.isClearOriginalTable()) {
                JdbcHelper.execute(connection, type.createClearTableSql(table));
            }

            if (mapping.isRequiresMerge()) {
                targetTable = JdbcHelper.getSchemaName(table) + "." + JdbcHelper.getTableName(table) + "_XPipeTemp";
                JdbcHelper.execute(
                        connection,
                        type.createTableLikeSql(
                                targetTable,
                                table,
                                mapping.getInformation().getTemporaryTableColumns(),
                                mapping.getInformation().getIdentifiers()));
            } else {
                targetTable = table;
            }

            updatePrepared = true;
        }

        @Override
        public void onClose(TableMapping mapping) throws Exception {
            try {
                if (mapping != null && ((JdbcTableParameterMap) mapping).isRequiresMerge()) {
                    try {
                        JdbcHelper.executeStatement(type.createTableMergeStatement(
                                connection, targetTable, table, (JdbcTableParameterMap) mapping));
                    } finally {
                        JdbcHelper.execute(connection, type.createTableDropSql(targetTable));
                    }
                }
            } finally {
                super.onClose(mapping);
            }
        }

        @Override
        protected PreparedStatement createInsertStatement(TableMapping mapping, ArrayNode node) throws SQLException {
            JdbcTableParameterMap parameterMap = (JdbcTableParameterMap) mapping;
            prepareTableUpdate(parameterMap);

            PreparedStatement statement = type.createUpsertStatement(connection, targetTable, parameterMap);

            for (DataStructureNode n : node.getNodes()) {
                AtomicInteger insertCounter = new AtomicInteger(1);
                Charsetter.FailableConsumer<Integer, SQLException> filler = tupleIndex -> {
                    var nodeAt = n.at(tupleIndex);
                    var columnIndex = parameterMap.map(tupleIndex).orElseThrow();
                    var category =
                            parameterMap.getInformation().getJdbcCategories().get(columnIndex);
                    category.setValue(
                            statement,
                            parameterMap.getInformation().getJdbcDataTypes().get(columnIndex),
                            insertCounter.get(),
                            nodeAt);

                    insertCounter.getAndIncrement();
                };

                type.fillUpsertStatement(statement, n.asTuple(), parameterMap, filler);
                if (node.size() > 1) {
                    statement.addBatch();
                }
            }

            return statement;
        }
    }
}
