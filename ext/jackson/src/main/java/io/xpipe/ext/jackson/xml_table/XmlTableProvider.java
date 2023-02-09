package io.xpipe.ext.jackson.xml_table;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.impl.TextSource;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.base.SimpleFileDataSourceProvider;
import io.xpipe.ext.jackson.xml.XmlProvider;
import io.xpipe.extension.util.DialogHelper;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import io.xpipe.extension.util.UniformDataSourceProvider;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XmlTableProvider
        implements UniformDataSourceProvider<XmlTableProvider.Source>,
                SimpleFileDataSourceProvider<XmlTableProvider.Source> {

    @Override
    public boolean shouldShow(DataSourceType type) {
        // Prefer XML structure over table if type is unknown
        return type != null;
    }

    @Override
    public boolean prefersStore(DataStore store, DataSourceType type) {
        // Prefer XML structure over table if type is unknown
        if (type == null) {
            return false;
        }

        return SimpleFileDataSourceProvider.super.prefersStore(store, type);
    }

    @Override
    public boolean supportsConversion(Source in, DataSourceType t) {
        if (t == DataSourceType.TEXT || t == DataSourceType.STRUCTURE) {
            return true;
        }

        return SimpleFileDataSourceProvider.super.supportsConversion(in, t);
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

        if (t == DataSourceType.STRUCTURE) {
            return XmlProvider.Source.builder()
                    .store(in.getStore())
                    .charset(in.getCharset())
                    .newLine(in.getNewLine())
                    .build();
        }

        return SimpleFileDataSourceProvider.super.convert(in, t);
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.TABLE;
    }

    @Override
    public Class<XmlTableProvider.Source> getSourceClass() {
        return XmlTableProvider.Source.class;
    }

    @Override
    public String getId() {
        return "xmlTable";
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("xml-table");
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        var map = new LinkedHashMap<String, List<String>>();
        map.put(i18nKey("fileName"), List.of("xml"));
        return map;
    }

    @Override
    public Region configGui(Property<Source> source, boolean preferQuiet) {
        var charset = new SimpleObjectProperty<StreamCharset>(source.getValue().getCharset());
        var newLine = new SimpleObjectProperty<NewLine>(source.getValue().getNewLine());
        var rootName = new SimpleStringProperty(source.getValue().getRootName());
        var entryName = new SimpleStringProperty(source.getValue().getEntryName());
        return new DynamicOptionsBuilder()
                .addCharset(charset)
                .addNewLine(newLine)
                .bind(
                        () -> {
                            return Source.builder()
                                    .store(source.getValue().getStore())
                                    .newLine(newLine.getValue())
                                    .charset(charset.getValue())
                                    .rootName(rootName.get())
                                    .entryName(entryName.get())
                                    .build();
                        },
                        source)
                .build();
    }

    @Override
    public Dialog configDialog(XmlTableProvider.Source source, boolean all) {
        var cs = DialogHelper.charsetQuery(source.getCharset(), all);
        var nl = DialogHelper.newLineQuery(source.getNewLine(), all);
        var rootNameQuery = Dialog.query("Root Name", false, true, !all, source.getRootName(), QueryConverter.STRING);
        var entryNameQuery =
                Dialog.query("Entry Name", false, true, !all, source.getEntryName(), QueryConverter.STRING);
        return Dialog.chain(cs, nl, rootNameQuery, entryNameQuery).evaluateTo(() -> Source.builder()
                .store(source.getStore())
                .newLine(nl.getResult())
                .charset(cs.getResult())
                .rootName(rootNameQuery.getResult())
                .entryName(entryNameQuery.getResult())
                .build());
    }

    @Override
    public XmlTableProvider.Source createDefaultSource(DataStore input) throws Exception {
        var cs = Charsetter.get().detect((StreamDataStore) input);
        var tableNames = XmlTableReadConnection.detect(input.asNeeded(), cs.getCharset());
        return Source.builder()
                .store(input.asNeeded())
                .newLine(cs.getNewLine())
                .charset(cs.getCharset())
                .rootName(tableNames.rootName())
                .entryName(tableNames.entryName())
                .build();
    }

    public String getDisplayIconFileName() {
        return "jackson:xml_icon.png";
    }

    @JsonTypeName("xmlTable")
    @SuperBuilder
    @Jacksonized
    @Getter
    public static class Source extends TableDataSource<StreamDataStore> {

        StreamCharset charset;
        NewLine newLine;
        String rootName;
        String entryName;

        @Override
        protected TableWriteConnection newWriteConnection(WriteMode mode) {
            return new XmlTableWriteConnection(this);
        }

        @Override
        protected TableReadConnection newReadConnection() {
            return new XmlTableReadConnection(this);
        }
    }
}
