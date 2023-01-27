package io.xpipe.ext.jdbc;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.data.node.TupleNode;
import io.xpipe.ext.jdbc.source.JdbcTableParameterMap;
import io.xpipe.extension.util.ModuleLayerLoader;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.List;
import java.util.ServiceLoader;
import java.util.Set;

public interface JdbcDialect {

    Set<JdbcDialect> DIALECT = new HashSet<>();

    public static class Loader implements ModuleLayerLoader {

        @Override
        public void init(ModuleLayer layer) {
            ServiceLoader.load(layer, JdbcDialect.class).stream().forEach(moduleLayerLoaderProvider -> {
                DIALECT.add(moduleLayerLoaderProvider.get());
            });
        }

        @Override
        public boolean requiresFullDaemon() {
            return false;
        }
    }

    static JdbcDialect getDialect(Connection connection) {
        return DIALECT.stream().filter(jdbcDialect -> {
            try {
                return jdbcDialect.matches(connection);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        }).findAny().orElseThrow(() -> {
            return new IllegalArgumentException("No dialect found");
        });
    }

    default JdbcDataTypeCategory getCategory(ResultSet result, int index) throws SQLException {
        return JdbcDataTypeCategory.getApplicableCategory(this, result, index);
    }

    boolean matches(Connection connection) throws SQLException;

    default List<JdbcDataTypeCategory> getAdditionalCategories() {
        return List.of();
    }

    PreparedStatement createTableMergeStatement(
            Connection connection, String source, String target, JdbcTableParameterMap parameterMap)
            throws SQLException;

    PreparedStatement createUpsertStatement(Connection connection, String table, JdbcTableParameterMap map)
            throws SQLException;

    default PreparedStatement createTableSelectStatement(
            Connection connection, String tableName, List<String> identifiers) throws SQLException {
        var order = String.join(",", identifiers);
        var orderBy = identifiers.size() > 0 ? String.format("ORDER BY %s", order) : "";
        var statement = connection.prepareStatement(String.format(
                """
                                                                          SELECT *
                                                                          FROM %s
                                                                          %s
                                                                          """,
                tableName, orderBy));
        return statement;
    }

    void disableConstraints(Connection connection) throws SQLException;

    void enableConstraints(Connection connection) throws SQLException;

    default String createQueryAllSql(String table) throws SQLException {
        return "SELECT * FROM " + table + " WHERE 0=1";
    }

    default String createClearTableSql(String table) throws SQLException {
        return "DELETE FROM " + table;
    }

    default List<String> determineAdditionalGeneratedColumns(Connection connection, String table, List<String> columns)
            throws SQLException {

        return List.of();
    }

    PreparedStatement fillUpsertStatement(
            PreparedStatement statement,
            TupleNode tuple,
            JdbcTableParameterMap parameterMap,
            Charsetter.FailableConsumer<Integer, SQLException> filler)
            throws SQLException;

    default List<String> determineStandardTables(Connection connection, List<String> tables) throws SQLException {
        return tables;
    }

    default String createTableLikeSql(
            String newTable, String oldTable, List<String> columns, List<String> identifiers) {
        return String.format("CREATE TABLE %s LIKE %s", newTable, oldTable);
    }

    default String createTableDropSql(String table) {
        return String.format("DROP TABLE IF EXISTS %s", table);
    }
}
