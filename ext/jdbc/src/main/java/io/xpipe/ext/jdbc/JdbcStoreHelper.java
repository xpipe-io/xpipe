package io.xpipe.ext.jdbc;

import io.xpipe.core.util.SimpleProxyFunction;
import io.xpipe.ext.jdbc.source.JdbcSource;
import io.xpipe.ext.jdbc.source.JdbcTableSource;
import io.xpipe.extension.fxcomps.util.SimpleChangeListener;
import io.xpipe.extension.util.ThreadHelper;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public interface JdbcStoreHelper {

    public static Map<String, ObservableValue<String>> getAvailableDatabases(JdbcSource source) {
        return new ListAvailableDatabasesFunction(source.getStore())
                .callAndGet().stream().collect(Collectors.toMap(v -> v, v -> new SimpleStringProperty(v)));
    }

    public static ObservableValue<Map<String, ObservableValue<String>>> databaseChoiceMap(
            Property<? extends JdbcSource> source) {
        var databasesProperty = new SimpleObjectProperty<Map<String, ObservableValue<String>>>(
                source.getValue().getDatabase() != null
                        ? Map.of(
                                source.getValue().getDatabase(),
                                new SimpleStringProperty(source.getValue().getDatabase()))
                        : Map.of());
        ThreadHelper.runAsync(() -> {
            Map<String, ObservableValue<String>> databases = new ListAvailableDatabasesFunction(
                            source.getValue().getStore())
                    .callAndGet().stream().collect(Collectors.toMap(v -> v, v -> new SimpleStringProperty(v)));
            databasesProperty.set(databases);
        });
        return databasesProperty;
    }

    public static String getDefaultTable(JdbcStore store, String database) {
        var tables =
                database != null ? new ListAvailableTablesFunction(store, database).callAndGet() : List.<String>of();
        var table = tables.size() > 0 ? tables.get(0) : null;
        return table;
    }

    public static ObservableValue<Map<String, ObservableValue<String>>> tableChoiceMap(
            Property<JdbcTableSource> source, ObservableValue<String> database) {
        var tableProperty = new SimpleObjectProperty<Map<String, ObservableValue<String>>>(
                source.getValue().getTable() != null
                        ? Map.of(
                                source.getValue().getTable(),
                                new SimpleStringProperty(source.getValue().getTable()))
                        : Map.of());
        SimpleChangeListener.apply(database, val -> {
            ThreadHelper.runAsync(() -> {
                Map<String, ObservableValue<String>> tables = new ListAvailableTablesFunction(
                                source.getValue().getStore(), val)
                        .callAndGet().stream().collect(Collectors.toMap(v -> v, v -> new SimpleStringProperty(v)));
                tableProperty.set(tables);
            });
        });
        return tableProperty;
    }

    @NoArgsConstructor
    class GetDefaultDatabaseFunction extends SimpleProxyFunction<String> {

        private JdbcStore store;
        private String selected;

        public GetDefaultDatabaseFunction(JdbcStore store) {
            this.store = store;
        }

        public void callLocal() {
            var function = new ListAvailableDatabasesFunction(store);
            function.callLocal();
            var available = function.getResult();
            if (available.size() == 0) {
                return;
            }

            var selectedDatabaseFunction = new GetSelectedDatabaseFunction(store);
            selectedDatabaseFunction.callLocal();
            selected = selectedDatabaseFunction.getResult();
        }
    }

    @NoArgsConstructor
    class GetSelectedDatabaseFunction extends SimpleProxyFunction<String> {

        private JdbcStore store;
        private String database;

        public GetSelectedDatabaseFunction(JdbcStore store) {
            this.store = store;
        }

        @SneakyThrows
        public void callLocal() {
            try (Connection con = store.createConnection()) {
                var catalog = con.getCatalog();
                if (catalog == null || catalog.trim().length() == 0) {
                    return;
                }
                database = catalog;
            }
        }
    }

    @NoArgsConstructor
    class ListAvailableDatabasesFunction extends SimpleProxyFunction<List<String>> {

        private JdbcStore store;
        private List<String> databases;

        public ListAvailableDatabasesFunction(JdbcStore store) {
            this.store = store;
        }

        @Override
        @SneakyThrows
        public void callLocal() {
            databases = new ArrayList<String>();
            try (var con = store.createConnection()) {
                DatabaseMetaData meta = con.getMetaData();
                ResultSet res = meta.getCatalogs();
                while (res.next()) {
                    databases.add(res.getString(1));
                }
            }
        }
    }

    @NoArgsConstructor
    class ListAvailableTablesFunction extends SimpleProxyFunction<List<String>> {

        private JdbcStore store;
        private String database;
        private List<String> tables;

        public ListAvailableTablesFunction(JdbcStore store, String database) {
            this.store = store;
            this.database = database;
        }

        @Override
        @SneakyThrows
        public void callLocal() {
            var list = new ArrayList<String>();
            try (var con = store.createConnection()) {
                con.setCatalog(database);
                DatabaseMetaData meta = con.getMetaData();
                ResultSet res = meta.getTables(database, null, "%", null);
                while (res.next()) {
                    var type = res.getString(4);
                    var schema = res.getString(2);
                    if (type == null || !type.equals("TABLE")) {
                        continue;
                    }
                    var prefix = schema != null ? schema + "." : "";
                    list.add(prefix + res.getString(3));
                }
                tables = JdbcDialect.getDialect(con).determineStandardTables(con, list);
            }
        }
    }
}
