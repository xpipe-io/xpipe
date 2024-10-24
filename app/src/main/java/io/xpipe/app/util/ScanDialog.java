package io.xpipe.app.util;

import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.process.ShellControl;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.core.store.ShellValidationContext;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static javafx.scene.layout.Priority.ALWAYS;

class ScanDialog extends DialogComp {

    private final DataStoreEntryRef<ShellStore> initialStore;
    private final BiFunction<DataStoreEntry, ShellControl, List<ScanProvider.ScanOperation>> applicable;
    private final Stage window;
    private final ObjectProperty<DataStoreEntryRef<ShellStore>> entry;
    private final ListProperty<ScanProvider.ScanOperation> selected =
            new SimpleListProperty<>(FXCollections.observableArrayList());
    private final BooleanProperty busy = new SimpleBooleanProperty();
    private ShellValidationContext shellValidationContext;

    ScanDialog(
            Stage window,
            DataStoreEntryRef<ShellStore> entry,
            BiFunction<DataStoreEntry, ShellControl, List<ScanProvider.ScanOperation>> applicable,
            ShellValidationContext shellValidationContext) {
        this.window = window;
        this.initialStore = entry;
        this.entry = new SimpleObjectProperty<>(entry);
        this.applicable = applicable;
        this.shellValidationContext = shellValidationContext;
    }

    @Override
    protected ObservableValue<Boolean> busy() {
        return busy;
    }

    @Override
    protected void finish() {
        ThreadHelper.runFailableAsync(() -> {
            try {
                if (entry.get() == null) {
                    return;
                }

                Platform.runLater(() -> {
                    window.close();
                });

                BooleanScope.executeExclusive(busy, () -> {
                    entry.get().get().setExpanded(true);
                    var copy = new ArrayList<>(selected);
                    for (var a : copy) {
                        // If the user decided to remove the selected entry
                        // while the scan is running, just return instantly
                        if (!DataStorage.get()
                                .getStoreEntriesSet()
                                .contains(entry.get().get())) {
                            return;
                        }

                        // Previous scan operation could have exited the shell
                        shellValidationContext.get().start();

                        try {
                            a.getScanner().run();
                        } catch (Throwable ex) {
                            ErrorEvent.fromThrowable(ex).handle();
                        }
                    }
                });
            } finally {
                if (shellValidationContext != null) {
                    shellValidationContext.close();
                    shellValidationContext = null;
                }
            }
        });
    }

    @Override
    protected void discard() {
        ThreadHelper.runAsync(() -> {
            if (shellValidationContext != null) {
                shellValidationContext.close();
                shellValidationContext = null;
            }
        });
    }

    @Override
    protected Comp<?> pane(Comp<?> content) {
        return content;
    }

    @Override
    public Comp<?> content() {
        StackPane stackPane = new StackPane();
        stackPane.getStyleClass().add("scan-list");

        var b = new OptionsBuilder()
                .name("scanAlertChoiceHeader")
                .description("scanAlertChoiceHeaderDescription")
                .addComp(new DataStoreChoiceComp<>(
                                DataStoreChoiceComp.Mode.OTHER,
                                null,
                                entry,
                                ShellStore.class,
                                store1 -> true,
                                StoreViewState.get().getAllConnectionsCategory())
                        .disable(new SimpleBooleanProperty(initialStore != null)))
                .name("scanAlertHeader")
                .description("scanAlertHeaderDescription")
                .addComp(Comp.of(() -> stackPane).vgrow())
                .buildComp()
                .prefWidth(500)
                .prefHeight(680)
                .apply(struc -> {
                    VBox.setVgrow(struc.get().getChildren().get(1), ALWAYS);
                })
                .padding(new Insets(5, 20, 20, 20));

        entry.subscribe(newValue -> {
            onUpdate(newValue, stackPane);
        });

        return b;
    }

    private void onUpdate(DataStoreEntryRef<ShellStore> newValue, StackPane stackPane) {
        selected.clear();
        stackPane.getChildren().clear();

        if (newValue == null) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                if (shellValidationContext != null) {
                    shellValidationContext.close();
                    shellValidationContext = null;
                }

                shellValidationContext = new ShellValidationContext(
                        newValue.getStore().getOrStartSession().withoutLicenseCheck().start());

                // Handle window close while connection is established
                if (!window.isShowing()) {
                    discard();
                    return;
                }

                var a = applicable.apply(entry.get().get(), shellValidationContext.get());

                Platform.runLater(() -> {
                    if (a == null) {
                        window.close();
                        return;
                    }

                    selected.setAll(a.stream()
                            .filter(scanOperation -> scanOperation.isDefaultSelected() && !scanOperation.isDisabled())
                            .toList());
                    Function<ScanProvider.ScanOperation, String> nameFunc = (ScanProvider.ScanOperation s) -> {
                        var n = AppI18n.get(s.getNameKey());
                        if (s.getLicensedFeatureId() == null) {
                            return n;
                        }

                        var suffix = LicenseProvider.get().getFeature(s.getLicensedFeatureId());
                        return n
                                + suffix.getDescriptionSuffix()
                                        .map(d -> " (" + d + ")")
                                        .orElse("");
                    };
                    var r = new ListSelectorComp<>(
                                    a, nameFunc, selected, scanOperation -> scanOperation.isDisabled(), a.size() > 3)
                            .createRegion();
                    stackPane.getChildren().add(r);
                });
            });
        });
    }
}
