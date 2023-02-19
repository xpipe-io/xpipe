package io.xpipe.ext.jackson.xml;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.DialogHelper;
import io.xpipe.app.util.DynamicOptionsBuilder;
import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.impl.TextSource;
import io.xpipe.core.source.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.base.SimpleFileDataSourceProvider;
import io.xpipe.ext.jackson.xml_table.XmlTableProvider;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;
import lombok.Getter;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class XmlProvider implements SimpleFileDataSourceProvider<XmlProvider.Source> {
    public String getDisplayIconFileName() {
        return "jackson:xml_icon.png";
    }

    @Override
    public boolean supportsConversion(XmlProvider.Source in, DataSourceType t) {
        if (t == DataSourceType.TEXT || t == DataSourceType.TABLE) {
            return true;
        }

        return SimpleFileDataSourceProvider.super.supportsConversion(in, t);
    }

    @Override
    public DataSource<?> convert(XmlProvider.Source in, DataSourceType t) throws Exception {
        if (t == DataSourceType.TEXT) {
            return TextSource.builder()
                    .store(in.getStore())
                    .charset(in.getCharset())
                    .newLine(in.getNewLine())
                    .build();
        }

        if (t == DataSourceType.TABLE) {
            return XmlTableProvider.Source.builder()
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
        return List.of("xml");
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

    @Override
    public Dialog configDialog(XmlProvider.Source source, boolean all) {
        var cs = DialogHelper.charsetQuery(source.getCharset(), all);
        var nl = DialogHelper.newLineQuery(source.getNewLine(), all);
        return Dialog.chain(cs, nl).evaluateTo(() -> XmlProvider.Source.builder()
                .store(source.getStore())
                .charset(cs.getResult())
                .newLine(nl.getResult())
                .build());
    }

    @Override
    public XmlProvider.Source createDefaultSource(DataStore input) throws Exception {
        var cs = Charsetter.get().detect(input.asNeeded());
        return XmlProvider.Source.builder()
                .store(input.asNeeded())
                .charset(cs.getCharset())
                .newLine(cs.getNewLine())
                .build();
    }

    @JsonTypeName("xml")
    @SuperBuilder
    @Jacksonized
    @Getter
    public static class Source extends StructureDataSource<StreamDataStore> {

        private final StreamCharset charset;
        private final NewLine newLine;

        @Override
        protected StructureWriteConnection newWriteConnection(WriteMode mode) {
            return new XmlWriteConnection(this);
        }

        @Override
        protected StructureReadConnection newReadConnection() {
            return new XmlReadConnection(this);
        }
    }
}
