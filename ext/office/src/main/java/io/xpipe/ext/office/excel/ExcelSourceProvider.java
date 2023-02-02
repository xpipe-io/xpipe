package io.xpipe.ext.office.excel;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.source.DataSourceType;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.StreamDataStore;
import io.xpipe.ext.base.SimpleFileDataSourceProvider;
import io.xpipe.ext.office.excel.model.ExcelHeaderState;
import io.xpipe.ext.office.excel.model.ExcelRange;
import io.xpipe.ext.office.excel.model.ExcelSheetIdentifier;
import io.xpipe.extension.I18n;
import io.xpipe.extension.util.DialogHelper;
import io.xpipe.extension.util.DynamicOptionsBuilder;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;
import org.apache.poi.openxml4j.util.ZipSecureFile;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

public class ExcelSourceProvider implements SimpleFileDataSourceProvider<ExcelSource> {

    @Override
    public void init() throws Exception {
        SimpleFileDataSourceProvider.super.init();

        ZipSecureFile.setMinInflateRatio(0.001);
    }

    @Override
    public DataSourceType getPrimaryType() {
        return DataSourceType.TABLE;
    }

    @Override
    public Map<String, List<String>> getSupportedExtensions() {
        return Map.of(i18nKey("fileName"), List.of("xlsx"));
    }

    @Override
    public Region configGui(Property<ExcelSource> source, boolean preferQuiet) throws Exception {
        var s = source.getValue();

        var headerState = new SimpleObjectProperty<ExcelHeaderState>(s.getHeaderState());
        var headerStateNames = new LinkedHashMap<ExcelHeaderState, ObservableValue<String>>();
        headerStateNames.put(ExcelHeaderState.INCLUDED, I18n.observable("excel.included"));
        headerStateNames.put(ExcelHeaderState.EXCLUDED, I18n.observable("excel.excluded"));

        var range =
                new SimpleObjectProperty<String>(source.getValue().getRange().toString());

        var availableSheets = ExcelHelper.getSheetIdentifiers(source.getValue().getStore());
        var sheetNames = new LinkedHashMap<ExcelSheetIdentifier, ObservableValue<String>>();
        availableSheets.forEach(identifier -> {
            sheetNames.put(
                    identifier,
                    new SimpleStringProperty(identifier.getName() + " (" + (identifier.getIndex() + 1) + ".)"));
        });
        var sheet = new SimpleObjectProperty<>(source.getValue().getIdentifier());

        var continueAfterSelection = new SimpleBooleanProperty(source.getValue().isContinueSelection());

        return new DynamicOptionsBuilder()
                .addChoice(sheet, I18n.observable("excel.sheet"), sheetNames, false)
                .addString("excel.range", range, true)
                .addToggle("excel.continueAfterSelection", continueAfterSelection)
                .addToggle(headerState, I18n.observable("excel.header"), headerStateNames)
                .bind(
                        () -> {
                            return ExcelSource.builder()
                                    .store(source.getValue().getStore())
                                    .identifier(sheet.get())
                                    .headerState(headerState.get())
                                    .range(ExcelRange.parse(range.get()))
                                    .continueSelection(continueAfterSelection.get())
                                    .build();
                        },
                        source)
                .build();
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("excel", "xlsx", ".xlsx");
    }

    public Dialog configDialog(ExcelSource source, boolean preferQuiet) {
        AtomicReference<ExcelSource> editedSource = new AtomicReference<>(source);
        var sheetQ = Dialog.lazy(() -> {
            var availableSheets = new ArrayList<>(ExcelHelper.getSheetIdentifiers(source.getStore()));
            if (availableSheets.size() == 0) {
                availableSheets.add(source.getIdentifier());
            }

            return Dialog.skipIf(
                            Dialog.choice(
                                    "Sheet",
                                    o -> o.getName(),
                                    true,
                                    false,
                                    source.getIdentifier(),
                                    availableSheets.toArray(ExcelSheetIdentifier[]::new)),
                            () -> availableSheets.size() <= 1)
                    .onCompletion((ExcelSheetIdentifier id) -> {
                        if (id != editedSource.get().getIdentifier()) {
                            editedSource.set(ExcelDetector.detect(source.getStore(), id));
                        }
                    });
        });

        var rangeQ = Dialog.lazy(() -> DialogHelper.query(
                "Range",
                editedSource.get().getRange(),
                false,
                new QueryConverter<>() {
                    @Override
                    protected ExcelRange fromString(String s) {
                        return ExcelRange.parse(s);
                    }

                    @Override
                    protected String toString(ExcelRange value) {
                        return value.toString();
                    }
                },
                preferQuiet));

        var headerQ = Dialog.lazy(() -> Dialog.choice(
                "Header",
                (ExcelHeaderState h) -> h == ExcelHeaderState.INCLUDED ? "Included" : "Excluded",
                true,
                preferQuiet,
                editedSource.get().getHeaderState(),
                ExcelHeaderState.values()));

        var continueQ = Dialog.lazy(() -> DialogHelper.booleanChoice(
                "Continue Selection", editedSource.get().isContinueSelection(), preferQuiet));

        return Dialog.chain(Dialog.busy(), sheetQ, rangeQ, headerQ, continueQ).evaluateTo(() -> ExcelSource.builder()
                .store(source.getStore())
                .range(rangeQ.getResult())
                .identifier(sheetQ.getResult())
                .continueSelection(continueQ.getResult())
                .headerState(headerQ.getResult())
                .build());
    }

    @Override
    public ExcelSource createDefaultSource(DataStore input) throws Exception {
        var stream = (StreamDataStore) input;
        return ExcelDetector.detect(stream);
    }

    @Override
    public Class<ExcelSource> getSourceClass() {
        return ExcelSource.class;
    }
}
