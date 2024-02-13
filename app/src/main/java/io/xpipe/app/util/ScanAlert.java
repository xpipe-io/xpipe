package io.xpipe.app.util;

import io.xpipe.app.comp.base.DialogComp;
import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.ShellStore;
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
import java.util.function.Function;

import static javafx.scene.layout.Priority.ALWAYS;

public class ScanAlert {

    public static void showAsync(DataStoreEntry entry) {
        ThreadHelper.runAsync(() -> {
            if (entry == null || entry.getStore() instanceof ShellStore) {
                showForShellStore(entry);
            }
        });
    }

    private static void showForShellStore(DataStoreEntry initial) {
        show(initial, (DataStoreEntry entry) -> {
            try (var sc = ((ShellStore) entry.getStore()).control().start()) {
                if (!sc.getShellDialect().getDumbMode().supportsAnyPossibleInteraction()) {
                    return null;
                }

                var providers = ScanProvider.getAll();
                var applicable = new ArrayList<ScanProvider.ScanOperation>();
                for (ScanProvider scanProvider : providers) {
                    try {
                        ScanProvider.ScanOperation operation = scanProvider.create(entry, sc);
                        if (operation != null) {
                            applicable.add(operation);
                        }
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable(ex).handle();
                    }
                }
                return applicable;
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
                return null;
            }
        });
    }

    private static class Dialog extends DialogComp {

        private final DataStoreEntryRef<ShellStore> initialStore;
        private final Function<DataStoreEntry, List<ScanProvider.ScanOperation>> applicable;
        private final Stage window;
        private final ObjectProperty<DataStoreEntryRef<ShellStore>> entry;
        private final ListProperty<ScanProvider.ScanOperation> selected = new SimpleListProperty<ScanProvider.ScanOperation>(FXCollections.observableArrayList());
        private final BooleanProperty busy = new SimpleBooleanProperty();

        private Dialog(Stage window, DataStoreEntryRef<ShellStore> entry, Function<DataStoreEntry, List<ScanProvider.ScanOperation>> applicable) {
            this.window = window;
            this.initialStore = entry;
            this.entry = new SimpleObjectProperty<>(entry);
            this.applicable = applicable;
        }

        @Override
        protected void finish() {
            ThreadHelper.runAsync(() -> {
                if (entry.get() == null) {
                    return;
                }

                Platform.runLater(() -> {
                    window.close();
                });

                BooleanScope.execute(busy, () -> {
                    entry.get().get().setExpanded(true);

                    var copy = new ArrayList<>(selected);
                    for (var a : copy) {
                        // If the user decided to remove the selected entry
                        // while the scan is running, just return instantly
                        if (!DataStorage.get().getStoreEntriesSet().contains(entry.get().get())) {
                            return;
                        }

                        try {
                            a.getScanner().run();
                        } catch (Exception ex) {
                            ErrorEvent.fromThrowable(ex).handle();
                        }
                    }
                });
            });
        }

        @Override
        protected ObservableValue<Boolean> busy() {
            return busy;
        }

        @Override
        public Comp<?> content() {
            StackPane stackPane = new StackPane();
            stackPane.getStyleClass().add("scan-list");

            var b = new OptionsBuilder().name("scanAlertChoiceHeader").description("scanAlertChoiceHeaderDescription").addComp(
                    new DataStoreChoiceComp<>(DataStoreChoiceComp.Mode.OTHER, null, entry, ShellStore.class, store1 -> true,
                            StoreViewState.get().getAllConnectionsCategory()).disable(
                            new SimpleBooleanProperty(initialStore != null))).name("scanAlertHeader").description(
                    "scanAlertHeaderDescription").addComp(Comp.of(() -> stackPane).vgrow()).buildComp().prefWidth(500).prefHeight(
                    650).apply(struc -> {
                VBox.setVgrow(struc.get().getChildren().get(1), ALWAYS);
            }).padding(new Insets(20));

            SimpleChangeListener.apply(entry, newValue -> {
                selected.clear();
                stackPane.getChildren().clear();

                if (newValue == null) {
                    return;
                }

                ThreadHelper.runAsync(() -> {
                    BooleanScope.execute(busy, () -> {
                        var a = applicable.apply(entry.get().get());

                        Platform.runLater(() -> {
                            if (a == null) {
                                window.close();
                                return;
                            }

                            selected.setAll(a.stream().filter(scanOperation -> scanOperation.isDefaultSelected() && !scanOperation.isDisabled()).toList());
                            var r = new ListSelectorComp<ScanProvider.ScanOperation>(a,
                                    scanOperation -> AppI18n.get(scanOperation.getNameKey()),
                                    selected,scanOperation -> scanOperation.isDisabled(),
                                    a.size() > 3).createRegion();
                            stackPane.getChildren().add(r);
                        });
                    });
                });
            });

            return b;
        }
    }

    private static void show(
            DataStoreEntry initialStore, Function<DataStoreEntry, List<ScanProvider.ScanOperation>> applicable
    ) {
        DialogComp.showWindow("scanAlertTitle", stage ->
                new Dialog(stage, initialStore != null ? initialStore.ref() : null,
                        applicable));
    }
}
