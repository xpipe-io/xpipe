package io.xpipe.app.util;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.base.ModalOverlayContentComp;
import io.xpipe.app.comp.store.StoreChoiceComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.process.ShellControl;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.ArrayList;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

import static javafx.scene.layout.Priority.ALWAYS;

class ScanDialog extends ModalOverlayContentComp {

    private final DataStoreEntryRef<ShellStore> initialStore;
    private final BiFunction<DataStoreEntry, ShellControl, List<ScanProvider.ScanOpportunity>> applicable;
    private final ObjectProperty<DataStoreEntryRef<ShellStore>> entry;
    private final ListProperty<ScanProvider.ScanOpportunity> selected =
            new SimpleListProperty<>(FXCollections.observableArrayList());
    private final BooleanProperty busy = new SimpleBooleanProperty();

    ScanDialog(
            DataStoreEntryRef<ShellStore> entry,
            BiFunction<DataStoreEntry, ShellControl, List<ScanProvider.ScanOpportunity>> applicable) {
        this.initialStore = entry;
        this.entry = new SimpleObjectProperty<>(entry);
        this.applicable = applicable;
    }

    @Override
    protected ObservableValue<Boolean> busy() {
        return busy;
    }

    protected void finish() {
        ThreadHelper.runFailableAsync(() -> {
            if (entry.get() == null) {
                return;
            }

            Platform.runLater(() -> {
                var modal = getModalOverlay();
                modal.close();
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
                    var sc = entry.get().getStore().getOrStartSession();

                    try {
                        a.getProvider().scan(entry.get().get(), sc);
                    } catch (Throwable ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                }
            });
        });
    }

    private void onUpdate(DataStoreEntryRef<ShellStore> newValue, StackPane stackPane) {
        selected.clear();
        stackPane.getChildren().clear();

        if (newValue == null) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                var sc = entry.get().getStore().getOrStartSession();
                var a = applicable.apply(entry.get().get(), sc);
                Platform.runLater(() -> {
                    if (a == null) {
                        var modal = getModalOverlay();
                        if (modal != null) {
                            modal.close();
                        }
                        return;
                    }

                    selected.setAll(a.stream()
                            .filter(scanOperation -> scanOperation.isDefaultSelected() && !scanOperation.isDisabled())
                            .toList());
                    Function<ScanProvider.ScanOpportunity, String> nameFunc = (ScanProvider.ScanOpportunity s) -> {
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

    @Override
    protected Region createSimple() {
        StackPane stackPane = new StackPane();
        stackPane.getStyleClass().add("scan-list");

        var b = new OptionsBuilder()
                .name("scanAlertChoiceHeader")
                .description("scanAlertChoiceHeaderDescription")
                .addComp(new StoreChoiceComp<>(
                        StoreChoiceComp.Mode.OTHER,
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
                });

        entry.subscribe(newValue -> {
            onUpdate(newValue, stackPane);
        });

        return b.createRegion();
    }
}
