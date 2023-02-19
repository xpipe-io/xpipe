package io.xpipe.ext.jackson.json;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.DialogHelper;
import io.xpipe.app.util.DynamicOptionsBuilder;
import io.xpipe.app.util.UniformDataSourceProvider;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.impl.TextSource;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.base.SimpleFileDataSourceProvider;
import io.xpipe.ext.jackson.json_table.JsonTableProvider;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

;

public class JsonProvider
        implements UniformDataSourceProvider<JsonProvider.Source>, SimpleFileDataSourceProvider<JsonProvider.Source> {

    public String getDisplayIconFileName() {
        return "jackson:json_icon.png";
    }

    @Override
    public boolean supportsConversion(Source in, DataSourceType t) {
        return t == DataSourceType.TABLE
                || t == DataSourceType.TEXT
                || SimpleFileDataSourceProvider.super.supportsConversion(in, t);
    }

    @Override
    public List<String> getSupportedMimeTypes() {
        return List.of("application/json");
    }

    @Override
    public DataSource<?> convert(Source in, DataSourceType t) throws Exception {
        if (t == DataSourceType.TEXT) {
            return TextSource.builder()
                    .store(in.getStore())
                    .charset(in.getCharset())
                    .newLine(in.getNewLine())
                    .build();
        }

        if (t == DataSourceType.TABLE) {
            return JsonTableProvider.Source.builder()
                    .store(in.getStore())
                    .charset(in.getCharset())
                    .newLine(in.getNewLine())
                    .build();
        }

        return SimpleFileDataSourceProvider.super.convert(in, t);
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.STRUCTURE;
    }

    @Override
    public Class<Source> getSourceClass() {
        return Source.class;
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("json");
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

    @JsonTypeName("json")
    @SuperBuilder
    @Jacksonized
    @Getter
    public static class Source extends StructureDataSource<StreamDataStore> {

        private final StreamCharset charset;
        private final NewLine newLine;

        @Override
        protected StructureWriteConnection newWriteConnection(WriteMode mode) {
            return new JsonWriteConnection(this);
        }

        @Override
        protected StructureReadConnection newReadConnection() {
            return new JsonReadConnection(this);
        }
    }
}
