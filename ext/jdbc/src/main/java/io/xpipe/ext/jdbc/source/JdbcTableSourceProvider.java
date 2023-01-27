package io.xpipe.ext.jdbc.source;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.jdbc.JdbcStore;
import io.xpipe.ext.jdbc.JdbcStoreHelper;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;

import java.util.List;

public class JdbcTableSourceProvider extends JdbcSourceProvider<JdbcTableSource> {

    @Override
    public Region configGui(Property<JdbcTableSource> source, boolean preferQuiet) {
        var database = new SimpleObjectProperty<String>(source.getValue().getDatabase());
        var databasesProperty = JdbcStoreHelper.databaseChoiceMap(source);

        var tablesProperty = JdbcStoreHelper.tableChoiceMap(source, database);
        var table = new SimpleObjectProperty<String>(source.getValue().getTable());

        return new DynamicOptionsBuilder(false)
                .addChoice(database, I18n.observable("jdbc.database"), databasesProperty, false)
                .addChoice(table, I18n.observable("jdbc.table"), tablesProperty, false)
                .bind(
                        () -> {
                            return JdbcTableSource.builder()
                                    .store(source.getValue().getStore())
                                    .database(database.get())
                                    .table(table.get())
                                    .build();
                        },
                        source)
                .build();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("table-query");
    }

    @Override
    public Dialog configDialog(JdbcTableSource source, boolean all) {
        var defaultDatabase = source.getDatabase();
        var databaseQuery = Dialog.query("Database", false, true, false, defaultDatabase, QueryConverter.STRING);
        var tableQuery = Dialog.lazy(() -> {
            var defaultTable = source.getTable() != null
                    ? source.getTable()
                    : new JdbcStoreHelper.ListAvailableTablesFunction(source.getStore(), databaseQuery.getResult())
                            .callAndGet().stream().findFirst().orElse(null);
            return Dialog.query("Table", false, true, false, defaultTable, QueryConverter.STRING);
        });
        return Dialog.chain(databaseQuery, tableQuery).evaluateTo(() -> JdbcTableSource.builder()
                .store(source.getStore())
                .database(databaseQuery.getResult())
                .table(tableQuery.getResult())
                .build());
    }

    @Override
    public Class<JdbcTableSource> getSourceClass() {
        return JdbcTableSource.class;
    }

    @Override
    public JdbcTableSource createDefaultSource(DataStore input) throws Exception {
        JdbcStore store = input.asNeeded();
        String defDb = new JdbcStoreHelper.GetDefaultDatabaseFunction(store).callAndGet();
        var table = JdbcStoreHelper.getDefaultTable(store, defDb);
        return JdbcTableSource.builder()
                .store(store)
                .database(defDb)
                .table(table)
                .build();
    }

    @Override
    public String getId() {
        return "jdbcTable";
    }
}
