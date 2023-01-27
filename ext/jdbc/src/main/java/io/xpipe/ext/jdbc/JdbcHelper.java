package io.xpipe.ext.jdbc;

import com.microsoft.sqlserver.jdbc.SQLServerPreparedStatement;
import io.xpipe.extension.event.TrackEvent;
import lombok.SneakyThrows;

import java.io.PrintWriter;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class JdbcHelper {

    public static String getSchemaName(String schemaTableName) {
        var split = schemaTableName.split("\\.");
        if (split.length == 1) {
            return null;
        }

        return split[0];
    }

    public static String getTableName(String schemaTableName) {
        var split = schemaTableName.split("\\.");
        if (split.length == 1) {
            return schemaTableName;
        }

        return split[1];
    }

    public static void print(ResultSet resultSet) throws SQLException {
        ResultSetMetaData rsmd = resultSet.getMetaData();
        int columnsNumber = rsmd.getColumnCount();
        while (resultSet.next()) {
            for (int i = 1; i <= columnsNumber; i++) {
                if (i > 1) {
                    System.out.print(",  ");
                }
                String columnValue = resultSet.getString(i);
                System.out.print(columnValue + " " + rsmd.getColumnName(i));
            }
            System.out.println("");
        }
    }

    public static void execute(Connection connection, String sql) throws SQLException {
        if (sql == null) {
            return;
        }

        var previousState = connection.getAutoCommit();
        connection.setAutoCommit(true);
        try (Statement ignored = connection.createStatement()) {
            TrackEvent.trace("jdbc", "Executing statement:\n" + sql + "\n");
            ignored.execute(sql);
        }
        connection.setAutoCommit(previousState);
    }

    @SneakyThrows
    private static String statementToString(PreparedStatement statement) {
        String string = null;
        if (statement instanceof SQLServerPreparedStatement s) {
            var field = s.getClass().getDeclaredField("userSQL");
            field.setAccessible(true);
            string = (String) field.get(s);
        } else {
            string = statement != null ? statement.toString() : "null";
        }
        return string;
    }

    @SneakyThrows
    public static void executeStatement(PreparedStatement statement) throws SQLException {
        try (Statement ignored = statement) {
            TrackEvent.trace("jdbc", "Executing statement:\n" + statementToString(statement) + "\n");
            if (statement != null) {
                statement.execute();
            }
        }
    }

    @SneakyThrows
    public static void executeBatchStatement(PreparedStatement statement) throws SQLException {
        try (Statement ignored = statement) {
            TrackEvent.trace("jdbc", "Executing batch statement:\n" + statementToString(statement) + "\n");
            statement.executeBatch();
        }
    }

    public static ResultSet executeQueryStatement(PreparedStatement statement) throws SQLException {
        TrackEvent.trace("jdbc", "Executing query statement:\n" + statementToString(statement) + "\n");
        return statement.executeQuery();
    }

    public static String executeSingletonQueryStatement(Connection connection, String s) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(s)) {
            var result = executeQueryStatement(statement);
            result.next();
            return result.getString(1);
        }
    }

    public static List<String> getTableColumnNames(Connection connection, String schemaTableName) throws SQLException {
        var list = new ArrayList<String>();
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet res = meta.getColumns(
                null, JdbcHelper.getSchemaName(schemaTableName), JdbcHelper.getTableName(schemaTableName), null);
        while (res.next()) {
            list.add(res.getString(4));
        }
        res.close();
        return list;
    }

    public static List<String> readSingleColumnResultSet(ResultSet result) throws SQLException {

        var resultsStrings = new ArrayList<String>();
        while (result.next()) {
            resultsStrings.add(result.getString(1));
        }
        return resultsStrings;
    }

    public static List<String> getGeneratedColumns(Connection connection, String schemaTableName, ResultSet resultSet)
            throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();

        var generated = new ArrayList<String>();
        try (ResultSet result = meta.getColumns(
                null, JdbcHelper.getSchemaName(schemaTableName), JdbcHelper.getTableName(schemaTableName), null)) {
            var counter = 1;
            while (result.next()) {
                if ("YES".equals(result.getString(23)) || "YES".equals(result.getString(24))) {
                    generated.add(result.getString(4));
                } else if (resultSet.getMetaData().isAutoIncrement(counter)) {
                    generated.add(result.getString(4));
                }
                counter++;
            }
        }
        return generated;
    }

    @SneakyThrows
    public static List<String> listAvailableTables(JdbcStore store,  String db) {
        try (var con = store.createConnection()) {
            con.setCatalog(db);
            return listAvailableTables(con);
        }
    }

    public static List<String> listAvailableTables(Connection connection) throws SQLException {
        var list = new ArrayList<String>();
        DatabaseMetaData meta = connection.getMetaData();
        ResultSet res = meta.getTables(null, null, "%", null);
        while (res.next()) {
            var type = res.getString(4);
            var schema = res.getString(2);
            if (type == null || !type.equals("TABLE")) {
                continue;
            }
            var prefix = schema != null ? schema + "." : "";
            list.add(prefix + res.getString(3));
        }
        res.close();
        return list;
    }

    public static List<String> getPrimaryKeys(Connection connection, String table) throws SQLException {
        DatabaseMetaData meta = connection.getMetaData();

        var primaryKeys = new ArrayList<String>();
        try (ResultSet result =
                meta.getPrimaryKeys(null, JdbcHelper.getSchemaName(table), JdbcHelper.getTableName(table))) {
            while (result.next()) {
                primaryKeys.add(result.getString(4));
            }
        }
        return primaryKeys;
    }

    public static void enableDebug() {
        DriverManager.setLogWriter(new PrintWriter(System.out));
    }

    public static String cleanConnectionUrl(String url, String protocol) {
        var cleaned = url;
        if (cleaned != null && cleaned.startsWith("jdbc:")) {
            cleaned = cleaned.substring(5);
        }
        if (cleaned != null && cleaned.startsWith(protocol + "://")) {
            cleaned = cleaned.substring(protocol.length() + 3);
        }
        return cleaned;
    }

    public static List<Integer> getColumnTypes(ResultSet result) throws SQLException {
        var list = new ArrayList<Integer>();
        try {
            for (int i = 0; i < result.getMetaData().getColumnCount(); i++) {
                list.add(result.getMetaData().getColumnType(i + 1));
            }
        } catch (SQLException e) {
            return List.of();
        }
        return list;
    }
}
