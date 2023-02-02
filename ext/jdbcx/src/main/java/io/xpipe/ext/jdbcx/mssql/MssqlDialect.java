package io.xpipe.ext.jdbcx.mssql;

import com.microsoft.sqlserver.jdbc.Geography;
import com.microsoft.sqlserver.jdbc.Geometry;
import com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement;
import com.microsoft.sqlserver.jdbc.SQLServerResultSet;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.data.node.DataStructureNode;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.core.data.node.ValueNode;
import io.xpipe.ext.jdbc.JdbcDataTypeCategory;
import io.xpipe.ext.jdbc.JdbcDialect;
import io.xpipe.ext.jdbc.JdbcHelper;
import io.xpipe.ext.jdbc.source.JdbcTableParameterMap;
import microsoft.sql.Types;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class MssqlDialect implements JdbcDialect {

    @Override
    public boolean matches(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().equals("Microsoft SQL Server");
    }

    private static final List<JdbcDataTypeCategory> ADDITIONAL_CATEGORIES = List.of(
            new JdbcDataTypeCategory.UserDefinedCategory(Types.GEOMETRY) {
                @Override
                public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
                    SQLServerResultSet sqlServerResultSet = (SQLServerResultSet) result;
                    var geometry = sqlServerResultSet.getGeometry(index);
                    return geometry != null ? ValueNode.of(geometry.STAsText()) : ValueNode.nullValue();
                }

                @Override
                public void setValueNonNull(
                        PreparedStatement statement, int jdbcDataType, int index, DataStructureNode value)
                        throws SQLException {
                    SQLServerPreparedStatement p = (SQLServerPreparedStatement) statement;
                    Geometry geometry = Geometry.parse(value.asString());
                    p.setGeometry(index, geometry);
                }
            },
            new JdbcDataTypeCategory.UserDefinedCategory(Types.GEOGRAPHY) {
                @Override
                public ValueNode getValue(ResultSet result, int jdbcDataType, int index) throws SQLException {
                    SQLServerResultSet sqlServerResultSet = (SQLServerResultSet) result;
                    var geography = sqlServerResultSet.getGeography(index);
                    return geography != null ? ValueNode.of(geography.STAsText()) : ValueNode.nullValue();
                }

                @Override
                public void setValueNonNull(
                        PreparedStatement statement, int jdbcDataType, int index, DataStructureNode value)
                        throws SQLException {
                    SQLServerPreparedStatement p = (SQLServerPreparedStatement) statement;
                    Geography geography = Geography.parse(value.asString());
                    p.setGeography(index, geography);
                }
            });

    @Override
    public List<JdbcDataTypeCategory> getAdditionalCategories() {
        return ADDITIONAL_CATEGORIES;
    }

    @Override
    public String createTableLikeSql(String newTable, String oldTable, List<String> columns, List<String> identifiers) {
        var select = columns != null ? String.join(",", columns) : "*";
        return String.format(
                """
                                     SELECT %s
                                     into %s
                                     FROM %s
                                     where 0=1
                                     union all
                                     SELECT %s
                                     FROM %s
                                     where 0=1""",
                select, newTable, oldTable, select, oldTable);
    }

    @Override
    public PreparedStatement createTableMergeStatement(
            Connection connection, String source, String target, JdbcTableParameterMap parameterMap)
            throws SQLException {
        var memoryOptimized = "1"
                .equals(JdbcHelper.executeSingletonQueryStatement(
                        connection,
                        String.format("SELECT OBJECTPROPERTY(OBJECT_ID('%s'),'TableIsMemoryOptimized')", target)));

        var equalJoinCheck = parameterMap.getInformation().getIdentifiers().stream()
                .map(s -> "T." + s + " = " + "S." + s)
                .collect(Collectors.joining(","));
        var insert = String.join(",", parameterMap.getInformation().getUpdateTableColumns());
        var columnList = parameterMap.getInformation().getUpdateTableColumns().stream()
                .map(s -> "S." + s)
                .collect(Collectors.joining(","));
        var update = parameterMap.getInformation().getUpdateTableColumns().stream()
                .map(s -> "T." + s + " = " + "S." + s)
                .collect(Collectors.joining(","));

        if (memoryOptimized) {
            var unequalJoinedCheck = parameterMap.getInformation().getIdentifiers().stream()
                    .map(s -> "T." + s + " <> " + "S." + s)
                    .collect(Collectors.joining(","));
            var query = String.format(
                    """
                                              UPDATE T
                                              SET %s
                                              FROM %s AS S, %s AS T
                                              WHERE %s

                                              INSERT INTO %s (%s)
                                              SELECT %s
                                              FROM %s S INNER JOIN %s T
                                              ON %s""",
                    update,
                    source,
                    target,
                    equalJoinCheck,
                    target,
                    insert,
                    columnList,
                    source,
                    target,
                    unequalJoinedCheck);
            return connection.prepareStatement(query);
        }

        var query = String.format(
                """
                                          MERGE %s AS T
                                          USING %s AS S
                                          ON %s
                                          WHEN NOT MATCHED BY TARGET THEN
                                              INSERT (%s)
                                              VALUES (%s)
                                          WHEN MATCHED THEN UPDATE SET
                                              %s
                                          WHEN NOT MATCHED BY SOURCE THEN
                                              DELETE;""",
                target, source, equalJoinCheck, insert, columnList, update);
        return connection.prepareStatement(query);
    }

    @Override
    public PreparedStatement createUpsertStatement(Connection connection, String table, JdbcTableParameterMap map)
            throws SQLException {
        var values = map.getInsertTableColumns().stream().map(s -> "?").collect(Collectors.joining(","));
        var equalJoinCheck = map.hasIdentifiers()
                ? map.getInformation().getIdentifiers().stream()
                        .map(s -> "" + s + " = " + "?")
                        .collect(Collectors.joining(","))
                : "0=1";
        var insert = String.join(",", map.getInsertTableColumns());
        var update = map.getInformation().getUpdateTableColumns().stream()
                .map(s -> "" + s + " = " + "?")
                .collect(Collectors.joining(","));

        var insertStatement = String.format("INSERT INTO %s (%s)\nVALUES (%s)", table, insert, values);
        var upsertStatement = String.format(
                """
                                                    UPDATE %s
                                                    SET %s
                                                    WHERE %s
                                                    IF @@ROWCOUNT = 0
                                                    %s""",
                table, update, equalJoinCheck, insertStatement);

        if (map.isCanPerformUpdates()) {
            return connection.prepareStatement(upsertStatement);
        } else {
            return connection.prepareStatement(insertStatement);
        }
    }

    @Override
    public void disableConstraints(Connection connection) throws SQLException {
        JdbcHelper.execute(connection, "EXEC sp_MSforeachtable \"ALTER TABLE ? NOCHECK CONSTRAINT ALL\"");
    }

    @Override
    public void enableConstraints(Connection connection) throws SQLException {
        JdbcHelper.execute(connection, "EXEC sp_MSforeachtable \"ALTER TABLE ? WITH CHECK CHECK CONSTRAINT ALL\"");
    }

    @Override
    public PreparedStatement fillUpsertStatement(
            PreparedStatement statement,
            TupleNode tuple,
            JdbcTableParameterMap parameterMap,
            Charsetter.FailableConsumer<Integer, SQLException> filler)
            throws SQLException {
        if (parameterMap.isCanPerformUpdates()) {
            for (int i = 0; i < tuple.getNodes().size(); i++) {
                if (parameterMap.map(i).isEmpty()
                        || !parameterMap
                                .getInformation()
                                .getUpdateTableColumns()
                                .contains(parameterMap
                                        .getInformation()
                                        .getAllTableColumns()
                                        .get(parameterMap.map(i).getAsInt()))) {
                    continue;
                }

                filler.accept(i);
            }

            for (String primaryKey : parameterMap.getInformation().getIdentifiers()) {
                var tupleIndex = parameterMap.getTupleIndexOfColumnName(primaryKey);
                filler.accept(tupleIndex);
            }
        }

        for (int i = 0; i < tuple.getNodes().size(); i++) {
            if (parameterMap.map(i).isEmpty()
                    || !parameterMap
                            .getInsertTableColumns()
                            .contains(parameterMap
                                    .getInformation()
                                    .getAllTableColumns()
                                    .get(parameterMap.map(i).getAsInt()))) {
                continue;
            }

            filler.accept(i);
        }

        return statement;
    }

    @Override
    public List<String> determineStandardTables(Connection connection, List<String> tables) throws SQLException {
        var alteredTables = new ArrayList<>(tables);
        try (PreparedStatement statement = connection.prepareStatement(
                """
                                                                               SELECT
                                                                                   OBJECT_SCHEMA_NAME(object_id) AS 'Table Schema',
                                                                                   OBJECT_NAME(object_id) AS 'Temporal Table',
                                                                                   OBJECT_NAME(history_table_id) AS 'History Table'
                                                                               FROM sys.tables
                                                                               WHERE temporal_type = 2""")) {
            var result = JdbcHelper.executeQueryStatement(statement);
            while (result.next()) {
                var schema = result.getString(1);
                var temporal = schema + "." + result.getString(3);
                alteredTables.remove(temporal);
            }
        }

        alteredTables.removeIf(s -> s.startsWith("sys."));
        return alteredTables;
    }

    @Override
    public List<String> determineAdditionalGeneratedColumns(Connection connection, String table, List<String> columns)
            throws SQLException {
        var alwaysGenerated = getColumnProperty(connection, table, columns, "GeneratedAlwaysType");
        var isIdentity = getColumnProperty(connection, table, columns, "IsIdentity");

        var list = new ArrayList<String>();

        for (int i = 0; i < columns.size(); i++) {
            var remove = false;
            if (!"0".equals(alwaysGenerated.get(i))) {
                remove = true;
            }
            if (!"0".equals(isIdentity.get(i))) {
                remove = true;
            }
            if (remove) {
                list.add(columns.get(i));
            }
        }
        return list;
    }

    public List<String> getColumnProperty(Connection connection, String table, List<String> columns, String name)
            throws SQLException {
        PreparedStatement s = connection.prepareStatement(String.format(
                """
                                                                                SELECT COLUMNPROPERTY(id, name, '%s')
                                                                                FROM sys.syscolumns
                                                                                WHERE id=OBJECT_ID('%s')
                                                                                ORDER BY colid""",
                name, table));
        var list = new ArrayList<String>();
        try (ResultSet resultSet = JdbcHelper.executeQueryStatement(s)) {
            list.addAll(JdbcHelper.readSingleColumnResultSet(resultSet));
        }
        return list;
    }
}
