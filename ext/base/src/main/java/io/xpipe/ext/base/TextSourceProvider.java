package io.xpipe.ext.base;

import io.xpipe.core.charsetter.Charsetter;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.impl.TextSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import io.xpipe.extension.util.DialogHelper;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.layout.Region;

import java.util.List;
import java.util.Map;

public class TextSourceProvider implements SimpleFileDataSourceProvider<TextSource> {

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.TEXT;
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        return Map.of(i18nKey("fileName"), List.of("txt"));
    }

    @Override
    public List<String> getSupportedMimeTypes() {
        return List.of("text/plain");
    }

    @Override
    public Region configGui(Property<TextSource> source, boolean preferQuiet) {
        var charset = new SimpleObjectProperty<StreamCharset>(source.getValue().getCharset());
        var newLine = new SimpleObjectProperty<NewLine>(source.getValue().getNewLine());
        return new DynamicOptionsBuilder()
                .addCharset(charset)
                .addNewLine(newLine)
                .bind(
                        () -> {
                            return TextSource.builder()
                                    .store(source.getValue().getStore())
                                    .newLine(newLine.getValue())
                                    .charset(charset.getValue())
                                    .build();
                        },
                        source)
                .build();
    }

    @Override
    public Dialog configDialog(TextSource source, boolean all) {
        var cs = DialogHelper.charsetQuery(source.getCharset(), all);
        var nl = DialogHelper.newLineQuery(source.getNewLine(), all);
        return Dialog.chain(cs, nl).evaluateTo(() -> TextSource.builder()
                .store(source.getStore())
                .charset(cs.getResult())
                .newLine(nl.getResult())
                .build());
    }

    @Override
    public TextSource createDefaultSource(DataStore input) throws Exception {
        var cs = Charsetter.get().detect(input.asNeeded());
        return TextSource.builder()
                .store(input.asNeeded())
                .charset(cs.getCharset())
                .newLine(cs.getNewLine())
                .build();
    }

    @Override
    public Class<TextSource> getSourceClass() {
        return TextSource.class;
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("text", "txt", ".txt", ".text");
    }
}
