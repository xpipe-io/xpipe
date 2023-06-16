package io.xpipe.ext.csv;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.DialogHelper;
import io.xpipe.app.util.DynamicOptionsBuilder;
import io.xpipe.app.util.NamedCharacter;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.impl.TextSource;
import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.base.SimpleFileDataSourceProvider;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class CsvSourceProvider implements SimpleFileDataSourceProvider<CsvSource> {

    @Override
    public boolean supportsConversion(CsvSource in, DataSourceType t) {
        return t == DataSourceType.TEXT || SimpleFileDataSourceProvider.super.supportsConversion(in, t);
    }

    @Override
    public DataSource<?> convert(CsvSource in, DataSourceType t) throws Exception {
        return t == DataSourceType.TEXT
                ? TextSource.builder()
                        .store(in.getStore())
                        .charset(in.getCharset())
                        .newLine(in.getNewLine())
                        .build()
                : SimpleFileDataSourceProvider.super.convert(in, t);
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.TABLE;
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        return Map.of(i18nKey("fileName"), List.of("csv"));
    }

    @Override
    public Region configGui(Property<CsvSource> source, boolean preferQuiet) {
        var s = source.getValue();
        var charset = new SimpleObjectProperty<>(s.getCharset());
        var newLine = new SimpleObjectProperty<>(s.getNewLine());

        var headerState = new SimpleObjectProperty<>(s.getHeaderState());
        var headerStateNames = new LinkedHashMap<CsvHeaderState, ObservableValue<String>>();
        headerStateNames.put(CsvHeaderState.INCLUDED, AppI18n.observable("csv.included"));
        headerStateNames.put(CsvHeaderState.OMITTED, AppI18n.observable("csv.omitted"));

        var delimiter = new SimpleObjectProperty<>(s.getDelimiter());
        var delimiterNames = new LinkedHashMap<Character, ObservableValue<String>>();
        CsvDelimiter.ALL.forEach(d -> {
            delimiterNames.put(
                    d.getNamedCharacter().getCharacter(),
                    AppI18n.observable(d.getNamedCharacter().getTranslationKey()));
        });
        ObservableValue<String> delimiterCustom = AppI18n.observable("csv.custom");

        var quote = new SimpleObjectProperty<>(s.getQuote());
        var quoteNames = new LinkedHashMap<Character, ObservableValue<String>>();
        CsvQuoteChar.CHARS.forEach(d -> {
            quoteNames.put(d.getCharacter(), AppI18n.observable(d.getTranslationKey()));
        });

        return new DynamicOptionsBuilder()
                .addCharset(charset)
                .addNewLine(newLine)
                .addToggle(headerState, AppI18n.observable("csv.header"), headerStateNames)
                .addCharacter(delimiter, AppI18n.observable("csv.delimiter"), delimiterNames, delimiterCustom)
                .addCharacter(quote, AppI18n.observable("csv.quote"), quoteNames)
                .bind(
                        () -> {
                            return CsvSource.builder()
                                    .store(s.getStore())
                                    .charset(charset.get())
                                    .newLine(newLine.getValue())
                                    .delimiter(delimiter.get())
                                    .quote(quote.get())
                                    .headerState(headerState.get())
                                    .build();
                        },
                        source)
                .build();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("csv", ".csv");
    }

    public Dialog configDialog(CsvSource source, boolean all) {
        var cs = DialogHelper.charsetQuery(source.getCharset(), all);
        var nl = DialogHelper.newLineQuery(source.getNewLine(), all);
        var headerQ = Dialog.skipIf(
                Dialog.choice(
                        "Header",
                        (CsvHeaderState h) -> h == CsvHeaderState.INCLUDED ? "Included" : "Omitted",
                        true,
                        false,
                        source.getHeaderState(),
                        CsvHeaderState.values()),
                () -> source.getHeaderState() != null && !all);
        var quoteQ = DialogHelper.query(
                "Quote", source.getQuote(), false, NamedCharacter.converter(CsvQuoteChar.CHARS, false), all);
        var delimiterQ = DialogHelper.query(
                "Delimiter",
                source.getDelimiter(),
                false,
                NamedCharacter.converter(
                        CsvDelimiter.ALL.stream()
                                .map(CsvDelimiter::getNamedCharacter)
                                .toList(),
                        true),
                all);
        return Dialog.chain(cs, nl, headerQ, quoteQ, delimiterQ).evaluateTo(() -> CsvSource.builder()
                .store(source.getStore())
                .charset(cs.getResult())
                .newLine(nl.getResult())
                .delimiter(delimiterQ.getResult())
                .quote(quoteQ.getResult())
                .headerState(headerQ.getResult())
                .build());
    }

    @Override
    public CsvSource createDefaultSource(DataStore input) throws Exception {
        var stream = (StreamDataStore) input;
        return CsvDetector.detect(stream, 100);
    }

    @Override
    public Class<CsvSource> getSourceClass() {
        return CsvSource.class;
    }
}