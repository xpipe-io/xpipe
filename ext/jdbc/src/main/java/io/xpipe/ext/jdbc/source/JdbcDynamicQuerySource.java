package io.xpipe.ext.jdbc.source;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.source.TextDataSource;
import io.xpipe.core.store.DataStore;
import io.xpipe.ext.jdbc.JdbcStore;
import io.xpipe.ext.jdbc.JdbcStoreHelper;
import io.xpipe.extension.DataSourceProviders;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.DialogHelper;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.Validators;
import io.xpipe.extension.util.XPipeDaemon;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@JsonTypeName("jdbcDynamicQuery")
@SuperBuilder
@Getter
@Jacksonized
public class JdbcDynamicQuerySource extends JdbcQuerySource {

    private final TextDataSource<?> source;

    @Override
    public void checkComplete() throws Exception {
        super.checkComplete();
        Validators.nonNull(source, "Source");
    }

    @Override
    public void validate() throws Exception {
        super.validate();
        source.validate();
    }

    @Override
    protected String createQuery() throws Exception {
        if (source == null || !source.getFlow().hasInput()) {
            return null;
        }

        try (var in = source.openReadConnection()) {
            in.init();
            return in.readAll();
        }
    }

    public static class Provider extends JdbcSourceProvider<JdbcDynamicQuerySource> {

        @Override
        public Region configGui(Property<JdbcDynamicQuerySource> source, boolean preferQuiet) {
            var sourceProperty = new SimpleObjectProperty<>(source.getValue().getSource());
            var databaseProperty = new SimpleObjectProperty<>(source.getValue().getDatabase());
            return new DynamicOptionsBuilder(false)
                    .addChoice(
                            databaseProperty,
                            I18n.observable("jdbc.database"),
                            JdbcStoreHelper.getAvailableDatabases(source.getValue()),
                            false)
                    .addComp(
                            (ObservableValue<String>) null,
                            XPipeDaemon.getInstance()
                                    .namedSourceChooser(
                                            new SimpleObjectProperty<>(s -> s instanceof TextDataSource<?>),
                                            sourceProperty,
                                            Category.DATABASE),
                            sourceProperty)
                    .bind(
                            () -> {
                                return JdbcDynamicQuerySource.builder()
                                        .store(source.getValue().getStore())
                                        .database(databaseProperty.get())
                                        .source(sourceProperty.get())
                                        .build();
                            },
                            source)
                    .build();
        }

        @Override
        public Class<JdbcDynamicQuerySource> getSourceClass() {
            return JdbcDynamicQuerySource.class;
        }

        @Override
        public List<String> getPossibleNames() {
            return List.of("dynamic_query");
        }

        @Override
        public Dialog configDialog(JdbcDynamicQuerySource source, boolean all) {
            var databaseQuery =
                    Dialog.query("Database", false, true, false, source.getDatabase(), QueryConverter.STRING);
            var sourceQuery = DialogHelper.sourceQuery(
                    source.getSource(),
                    dataSource -> DataSourceProviders.byDataSourceClass(dataSource.getClass())
                                    .getPrimaryType()
                            == DataSourceType.TEXT);
            return Dialog.chain(databaseQuery, sourceQuery).evaluateTo(() -> {
                return JdbcDynamicQuerySource.builder()
                        .store(source.getStore())
                        .database(databaseQuery.getResult())
                        .source(sourceQuery.getResult())
                        .build();
            });
        }

        @Override
        public String getId() {
            return "jdbcDynamicQuery";
        }

        @Override
        public JdbcDynamicQuerySource createDefaultSource(DataStore input) throws Exception {
            JdbcStore store = input.asNeeded();
            String defDb = new JdbcStoreHelper.GetDefaultDatabaseFunction(store).callAndGet();
            return JdbcDynamicQuerySource.builder()
                    .store(input.asNeeded())
                    .database(defDb)
                    .build();
        }
    }
}
