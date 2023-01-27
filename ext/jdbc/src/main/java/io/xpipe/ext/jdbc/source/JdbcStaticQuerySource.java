package io.xpipe.ext.jdbc.source;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.jdbc.JdbcStore;
import io.xpipe.ext.jdbc.JdbcStoreHelper;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.Validators;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@SuperBuilder
@Jacksonized
@Getter
@JsonTypeName("jdbcStaticQuery")
public class JdbcStaticQuerySource extends JdbcQuerySource {

    private final String query;

    @Override
    public void checkComplete() throws Exception {
        super.checkComplete();
        Validators.nonNull(query, "Query");
        Validators.notEmpty(query, "Query");
    }

    @Override
    protected String createQuery() throws Exception {
        return query;
    }

    public static class Provider extends JdbcSourceProvider<JdbcStaticQuerySource> {

        @Override
        public Region configGui(Property<JdbcStaticQuerySource> source, boolean preferQuiet) {
            var queryQ = new SimpleObjectProperty<String>();
            var databaseProperty = new SimpleObjectProperty<>(source.getValue().getDatabase());
            var store = source.getValue().getStore();
            return new DynamicOptionsBuilder(false)
                    .addStringArea("query", queryQ, true)
                    .addChoice(
                            databaseProperty,
                            I18n.observable("jdbc.database"),
                            JdbcStoreHelper.getAvailableDatabases(source.getValue()),
                            false)
                    .bind(
                            () -> {
                                return builder()
                                        .store(store)
                                        .query(queryQ.get())
                                        .database(databaseProperty.get())
                                        .build();
                            },
                            source)
                    .build();
        }

        @Override
        public List<String> getPossibleNames() {
            return List.of("query", "static_query");
        }

        @Override
        public Dialog configDialog(JdbcStaticQuerySource source, boolean all) {
            var databaseQuery =
                    Dialog.query("Database", false, true, false, source.getDatabase(), QueryConverter.STRING);
            var stringQuery = Dialog.query("Query String", true, true, false, source.getQuery(), QueryConverter.STRING);
            return Dialog.chain(databaseQuery, stringQuery).evaluateTo(() -> {
                return builder()
                        .store(source.getStore())
                        .query(stringQuery.getResult())
                        .database(databaseQuery.getResult())
                        .build();
            });
        }

        @Override
        public Class<JdbcStaticQuerySource> getSourceClass() {
            return JdbcStaticQuerySource.class;
        }

        @Override
        public String getId() {
            return "jdbcStaticQuery";
        }

        @Override
        public JdbcStaticQuerySource createDefaultSource(DataStore input) throws Exception {
            JdbcStore store = input.asNeeded();
            String defDb = new JdbcStoreHelper.GetDefaultDatabaseFunction(store).callAndGet();
            return builder().store(input.asNeeded()).query("").database(defDb).build();
        }
    }
}
