package io.xpipe.ext.jackson.json_table;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.impl.TextSource;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.base.SimpleFileDataSourceProvider;
import io.xpipe.ext.jackson.json.JsonProvider;
import io.xpipe.extension.util.DialogHelper;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.UniformDataSourceProvider;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class JsonTableProvider
        implements UniformDataSourceProvider<JsonTableProvider.Source>,
                   SimpleFileDataSourceProvider<JsonTableProvider.Source> {

    @Override
    public boolean supportsConversion(JsonTableProvider.Source in, DataSourceType t) {
        if (t == DataSourceType.TEXT || t == DataSourceType.STRUCTURE) {
            return true;
        }

        return SimpleFileDataSourceProvider.super.supportsConversion(in, t);
    }

    @Override
    public DataSource<?> convert(JsonTableProvider.Source in, DataSourceType t) throws Exception {
        if (t == DataSourceType.TEXT) {
            return TextSource.builder()
                    .store(in.getStore())
                    .charset(in.getCharset())
                    .newLine(in.getNewLine())
                    .build();
        }

        if (t == DataSourceType.STRUCTURE) {
            return JsonProvider.Source.builder()
                    .store(in.getStore())
                    .charset(in.getCharset())
                    .newLine(in.getNewLine())
                    .build();
        }

        return SimpleFileDataSourceProvider.super.convert(in, t);
    }

    public String getDisplayIconFileName() {
        return "jackson:json_icon.png";
    }

    @Override
    public boolean shouldShow(DataSourceType type) {
        // Prefer Json structure over table if type is unknown
        return type != null;
    }

    @Override
    public boolean prefersStore(DataStore store, DataSourceType type) {
        // Prefer Json structure over table if type is unknown
        if (type == null) {
            return false;
        }

        return SimpleFileDataSourceProvider.super.prefersStore(store, type);
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.TABLE;
    }

    @Override
    public Class<JsonTableProvider.Source> getSourceClass() {
        return JsonTableProvider.Source.class;
    }

    @Override
    public String getId() {
        return "jsonTable";
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("json-table");
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        var map = new LinkedHashMap<String, List<String>>();
        map.put(i18nKey("fileName"), List.of("json"));
        return map;
    }

    @Override
    public Region configGui(Property<Source> source, boolean preferQuiet) {
        var charset = new SimpleObjectProperty<StreamCharset>(source.getValue().getCharset());
        var newLine = new SimpleObjectProperty<NewLine>(source.getValue().getNewLine());
        if (preferQuiet) {
            return new DynamicOptionsBuilder()
                    .addCharset(charset)
                    .addNewLine(newLine)
                    .bind(
                            () -> {
                                return Source.builder()
                                        .store(source.getValue().getStore())
                                        .newLine(newLine.getValue())
                                        .charset(charset.getValue())
                                        .build();
                            },
                            source)
                    .build();
        }
        return null;
    }

    @Override
    public Dialog configDialog(Source source, boolean all) {
        var cs = DialogHelper.charsetQuery(source.getCharset(), all);
        var nl = DialogHelper.newLineQuery(source.getNewLine(), all);
        return Dialog.chain(cs, nl).evaluateTo(() -> Source.builder()
                .store(source.getStore())
                .charset(cs.getResult())
                .newLine(nl.getResult())
                .build());
    }

    @Override
    public Source createDefaultSource(DataStore input) throws Exception {
        var cs = Charsetter.get().detect(input.asNeeded());
        return Source.builder()
                .store(input.asNeeded())
                .charset(cs.getCharset())
                .newLine(cs.getNewLine())
                .build();
    }

    @JsonTypeName("jsonTable")
    @SuperBuilder
    @Jacksonized
    @Getter
    public static class Source extends TableDataSource<StreamDataStore> {

        private final StreamCharset charset;
        private final NewLine newLine;

        @Override
        protected TableWriteConnection newWriteConnection(WriteMode mode) {
            return new JsonTableWriteConnection(this);
        }

        @Override
        protected TableReadConnection newReadConnection() {
            return new JsonTableReadConnection(this);
        }
    }
}
