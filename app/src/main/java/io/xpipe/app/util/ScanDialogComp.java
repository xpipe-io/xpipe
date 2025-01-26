package io.xpipe.app.util;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.comp.base.ModalOverlayContentComp;
import io.xpipe.app.comp.store.StoreChoiceComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.application.Platform;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.Collections;
import java.util.function.Function;

import static javafx.scene.layout.Priority.ALWAYS;

class ScanDialogComp extends ModalOverlayContentComp {

    private final DataStoreEntryRef<ShellStore> initialStore;
    private final ScanDialogAction action;
    private final ObjectProperty<DataStoreEntryRef<ShellStore>> entry;
    private final ObservableList<ScanProvider.ScanOpportunity> available = FXCollections.synchronizedObservableList(FXCollections.observableArrayList());
    private final ListProperty<ScanProvider.ScanOpportunity> selected =
            new SimpleListProperty<>(FXCollections.synchronizedObservableList(FXCollections.observableArrayList()));
    private final BooleanProperty busy = new SimpleBooleanProperty();

    ScanDialogComp(DataStoreEntryRef<ShellStore> entry, ScanDialogAction action) {
        this.initialStore = entry;
        this.entry = new SimpleObjectProperty<>(entry);
        this.action = action;
    }

    protected void finish() {
        ThreadHelper.runFailableAsync(() -> {
            if (entry.get() == null) {
                return;
            }

            Platform.runLater(() -> {
                var modal = getModalOverlay();
                if (modal != null) {
                    modal.close();
                }
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

    private void onUpdate(DataStoreEntryRef<ShellStore> newValue) {
        available.clear();
        selected.clear();

        if (newValue == null) {
            return;
        }

        ThreadHelper.runFailableAsync(() -> {
            BooleanScope.executeExclusive(busy, () -> {
                boolean r;
                try {
                    var sc = entry.get().getStore().getOrStartSession();
                    r = action.scan(available, selected, newValue.get(), sc);
                } catch (Throwable t) {
                    var modal = getModalOverlay();
                    if (initialStore != null && modal != null) {
                        modal.close();
                    }
                    throw t;
                }
                if (!r) {
                    var modal = getModalOverlay();
                    if (initialStore != null && modal != null) {
                        modal.close();
                    }
                }
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
                        .disable(busy.or(new SimpleBooleanProperty(initialStore != null))))
                .name("scanAlertHeader")
                .description("scanAlertHeaderDescription")
                .addComp(LoadingOverlayComp.noProgress(Comp.of(() -> stackPane), busy).vgrow())
                .buildComp()
                .prefWidth(500)
                .prefHeight(680)
                .apply(struc -> {
                    VBox.setVgrow(struc.get().getChildren().get(1), ALWAYS);
                });

        Function<ScanProvider.ScanOpportunity, String> nameFunc = (ScanProvider.ScanOpportunity s) -> {
            var n = AppI18n.get(s.getNameKey());
            if (s.getLicensedFeatureId() == null) {
                return n;
            }

            var suffix = LicenseProvider.get().getFeature(s.getLicensedFeatureId());
            return n + suffix.getDescriptionSuffix().map(d -> " (" + d + ")").orElse("");
        };
        var r = new ListSelectorComp<>(
                        available,
                        nameFunc,
                        selected,
                        scanOperation -> scanOperation.isDisabled(),
                        () -> available.size() > 3)
                .createRegion();
        stackPane.getChildren().add(r);

        entry.subscribe(newValue -> {
            onUpdate(newValue);
        });

        return b.createRegion();
    }
}
