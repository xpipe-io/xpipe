package io.xpipe.ext.jdbc.postgres;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.ext.jdbc.JdbcDataTypeCategory;
import io.xpipe.ext.jdbc.JdbcDialect;
import io.xpipe.ext.jdbc.JdbcHelper;
import io.xpipe.ext.jdbc.source.JdbcTableParameterMap;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class PostgresDialect implements JdbcDialect {

    @Override
    public JdbcDataTypeCategory getCategory(ResultSet result, int index) throws SQLException {
        return JdbcDialect.super.getCategory(result, index);
    }

    @Override
    public boolean matches(Connection connection) throws SQLException {
        return connection.getMetaData().getDatabaseProductName().equals("PostgreSQL");
    }

    @Override
    public PreparedStatement createTableMergeStatement(
            Connection connection, String source, String target, JdbcTableParameterMap parameterMap)
            throws SQLException {
        var where = parameterMap.getInformation().getIdentifiers().stream()
                .map(s -> "source." + escape(s) + " = " + "target." + escape(s) + "")
                .collect(Collectors.joining(" AND\n\t\t"));
        var delete = String.format(
                """
                                           DELETE FROM %s target
                                           WHERE NOT EXISTS (
                                               SELECT FROM %s source
                                               WHERE
                                                   %s
                                           );""",
                escapeTableName(target), escapeTableName(source), where);

        var insert = parameterMap.getInsertTableColumns().stream()
                .map(s -> escape(s))
                .collect(Collectors.joining(",\n\t"));
        var conflict = parameterMap.getInformation().getIdentifiers().stream()
                .map(s -> escape(s))
                .collect(Collectors.joining(","));
        var update = parameterMap.getInformation().getUpdateTableColumns().stream()
                .map(s -> escape(s) + " = " + "excluded." + s + "")
                .collect(Collectors.joining(",\n\t"));
        var insertStatement = String.format(
                """
                                                    INSERT INTO %s (
                                                        %s
                                                    )
                                                    SELECT %s
                                                    FROM %s""",
                escapeTableName(target), insert, insert, escapeTableName(source));
        var conflictStatement = String.format(
                """
                                                      ON CONFLICT (%s) DO UPDATE
                                                      SET
                                                          %s""",
                conflict, update);

        return connection.prepareStatement(delete + "\n" + insertStatement + "\n" + conflictStatement);
    }

    @Override
    public String createTableLikeSql(String newTable, String oldTable, List<String> columns, List<String> identifiers) {
        var select = columns != null ? columns.stream().map(s -> escape(s)).collect(Collectors.joining(",")) : "*";
        var keys = identifiers != null && identifiers.size() > 0
                ? identifiers.stream().map(s -> escape(s)).collect(Collectors.joining(","))
                : null;
        var alterTable = keys != null
                ? String.format("\nALTER TABLE %s ADD PRIMARY KEY (%s);", escapeTableName(newTable), keys)
                : "";
        var sql = String.format(
                "CREATE TABLE %s AS (SELECT %s FROM %s LIMIT 0);" + "%s;",
                escapeTableName(newTable), select, escapeTableName(oldTable), alterTable);

        return sql;
    }

    @Override
    public String createQueryAllSql(String table) throws SQLException {
        return "SELECT * FROM " + escapeTableName(table) + " WHERE 0=1";
    }

    @Override
    public String createTableDropSql(String table) {
        return String.format("DROP TABLE IF EXISTS %s", escapeTableName(table));
    }

    public PreparedStatement createTableSelectStatement(
            Connection connection, String tableName, List<String> identifiers) throws SQLException {
        var order = identifiers.stream().map(s -> escape(s)).collect(Collectors.joining(",\n\t"));
        var orderBy = identifiers.size() > 0
                ? String.format(
                        """
                                                                     ORDER BY (
                                                                         %s
                                                                     )""",
                        order)
                : "";
        var statement = connection.prepareStatement(String.format(
                """
                                                                          SELECT *
                                                                          FROM %s
                                                                          %s
                                                                          """,
                escapeTableName(tableName), orderBy));
        return statement;
    }

    @Override
    public void disableConstraints(Connection connection) throws SQLException {
        if (true) return;
        var allTables = JdbcHelper.listAvailableTables(connection);
        var tableArray = "["
                + allTables.stream().map(s -> "'" + escapeTableName(s) + "'").collect(Collectors.joining(",")) + "]";
        JdbcHelper.execute(
                connection,
                String.format(
                        """
                                                             DO $$
                                                             DECLARE t varchar;
                                                             BEGIN
                                                             FOREACH t IN ARRAY ARRAY%s LOOP
                                                                  EXECUTE 'ALTER TABLE ' || t || ' DISABLE TRIGGER ALL';
                                                             END LOOP;
                                                             END$$""",
                        tableArray));
    }

    @Override
    public void enableConstraints(Connection connection) throws SQLException {
        if (true) return;
        var allTables = JdbcHelper.listAvailableTables(connection);
        var tableArray = "["
                + allTables.stream().map(s -> "'" + escapeTableName(s) + "'").collect(Collectors.joining(",")) + "]";
        JdbcHelper.execute(
                connection,
                String.format(
                        """
                                                             DO $$
                                                             DECLARE t varchar;
                                                             BEGIN
                                                             FOREACH t IN ARRAY ARRAY%s LOOP
                                                                  EXECUTE 'ALTER TABLE ' || t || ' ENABLE TRIGGER ALL';
                                                             END LOOP;
                                                             END$$""",
                        tableArray));
    }

    @Override
    public PreparedStatement createUpsertStatement(Connection connection, String table, JdbcTableParameterMap map)
            throws SQLException {
        var values = map.getInsertTableColumns().stream().map(s -> "?").collect(Collectors.joining(",\n\t"));
        var insert = map.getInsertTableColumns().stream().map(s -> escape(s)).collect(Collectors.joining(",\n\t"));
        var conflict = map.getInformation().getIdentifiers().stream()
                .map(s -> escape(s))
                .collect(Collectors.joining(","));
        var update = map.getInformation().getUpdateTableColumns().stream()
                .map(s -> escape(s) + " = " + "excluded." + s + "")
                .collect(Collectors.joining(",\n\t"));

        var insertStatement = String.format(
                """
                                                    INSERT INTO %s (
                                                        %s
                                                    )
                                                    VALUES (
                                                        %s
                                                    )""",
                escapeTableName(table), insert, values);
        var statement = String.format(
                """
                                              %s
                                              ON CONFLICT (%s) DO UPDATE
                                              SET
                                                  %s
                                              """,
                insertStatement, conflict, update);
        return connection.prepareStatement(map.isCanPerformUpdates() ? statement : insertStatement);
    }

    @Override
    public PreparedStatement fillUpsertStatement(
            PreparedStatement statement,
            TupleNode tuple,
            JdbcTableParameterMap parameterMap,
            Charsetter.FailableConsumer<Integer, SQLException> filler)
            throws SQLException {
        //        if (false && parameterMap.isCanPerformUpdates()) {
        //            for (int i = 0; i < tuple.getNodes().size(); i++) {
        //                if (parameterMap.getNodeToColumnMap().get(i) == null
        //                        || !parameterMap
        //                                .getUpdateTableColumns()
        //                                .contains(parameterMap.getNodeToColumnMap().get(i))) {
        //                    continue;
        //                }
        //
        //                filler.accept(i);
        //            }
        //
        //            for (String primaryKey : parameterMap.getIdentifiers()) {
        //                var tupleIndex = parameterMap.getTupleIndexOfColumnName(primaryKey);
        //                filler.accept(tupleIndex);
        //            }
        //        }

        for (int i = 0; i < tuple.getNodes().size(); i++) {
            if (parameterMap.map(i).isEmpty()
                    || !parameterMap
                            .getInsertTableColumns()
                            .contains(parameterMap
                                    .getOutputType()
                                    .getNames()
                                    .get(parameterMap.map(i).getAsInt()))) {
                continue;
            }

            filler.accept(i);
        }

        return statement;
    }

    @Override
    public List<String> determineAdditionalGeneratedColumns(Connection connection, String table, List<String> columns)
            throws SQLException {
        var tableOid = getTableOid(connection, table);
        try (PreparedStatement statement = connection.prepareStatement(String.format(
                """
                        SELECT attname, attidentity, attgenerated
                        FROM pg_attribute
                        WHERE attnum > 0 AND attrelid = %s;""",
                tableOid))) {
            var result = JdbcHelper.executeQueryStatement(statement);
            var list = new ArrayList<String>();
            while (result.next()) {
                var identity = result.getString(2);
                var generated = result.getString(3);
                if (!identity.isEmpty() || !generated.isEmpty()) {
                    list.add(result.getString(1));
                }
            }
            return List.of();
        }
    }

    private String escapeTableName(String table) {
        return String.format("\"%s\".\"%s\"", JdbcHelper.getSchemaName(table), JdbcHelper.getTableName(table));
    }

    private String escape(String s) {
        return String.format("\"%s\"", s);
    }

    private String getTableOid(Connection connection, String table) throws SQLException {
        return JdbcHelper.executeSingletonQueryStatement(
                connection, String.format("SELECT '%s'::regclass::oid;", escapeTableName(table)));
    }

    @Override
    public String createClearTableSql(String table) throws SQLException {
        return String.format("DELETE FROM %s", escapeTableName(table));
    }
}
