package io.xpipe.ext.jdbc.test.item;

import io.xpipe.ext.jdbc.JdbcBaseStore;
import io.xpipe.ext.jdbc.JdbcHelper;
import io.xpipe.extension.test.TestModule;
import lombok.AllArgsConstructor;
import lombok.Value;
import org.junit.jupiter.api.Named;

import java.util.Collection;
import java.util.List;
import java.util.stream.Stream;

@Value
public class DatabaseItem {

    public static Stream<Named<DatabaseItem>> getAll() {
        return TestModule.getArguments(DatabaseItem.class, "io.xpipe.ext.jdbc.test.item.PrivateTestDatabaseItems");
    }

    @Value
    @AllArgsConstructor
    public static class TableItem {
        DatabaseItem item;
        String table;

        @Override
        public String toString() {
            return table;
        }

        public String getDatabase() {
            return item.getDatabase();
        }

        public JdbcBaseStore getStore() {
            return item.getStore();
        }
    }

    public static Stream<Named<TableItem>> getTableItems() {
        //  if (true) return List.of(new TableItem(POSTGRES.getStore(), POSTGRES.getDatabase(), "production.document"));
        return getAll().map(testDatabaseItemNamed -> testDatabaseItemNamed.getPayload())
                .map(item -> JdbcHelper.listAvailableTables(item.getStore(), item.getDatabase()).stream()
                        .filter(s -> !item.getExcludedTables().contains(s))
                        .map(s -> Named.of(item.getDatabase() + "/" + s, new TableItem(item, s)))
                        .toList())
                .flatMap(Collection::stream);
    }

    public static Stream<Named<TableItem>> getDisabledConstraintTableItems() {
        return getTableItems()
                .filter(tableItem -> tableItem.getPayload().getItem().canDisableConstraints);
    }

    JdbcBaseStore store;
    String database;
    List<String> excludedTables;
    int expectedTableCount;
    boolean canDisableConstraints;
}
