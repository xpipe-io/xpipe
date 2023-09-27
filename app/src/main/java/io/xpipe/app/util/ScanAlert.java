package io.xpipe.app.util;

import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.ShellStore;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

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
        show(initial != null ? initial.getStore().asNeeded() : null, (DataStoreEntry entry) -> {
            try (var sc = ((ShellStore) entry.getStore()).control().start()) {
                var providers = ScanProvider.getAll();
                var applicable = new ArrayList<ScanProvider.ScanOperation>();
                for (ScanProvider scanProvider : providers) {
                    ScanProvider.ScanOperation operation = scanProvider.create(entry, sc);
                    if (operation != null) {
                        applicable.add(operation);
                    }
                }
                return applicable;
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).handle();
                return null;
            }
        });
    }

    private static void show(
            ShellStore initialStore, Function<DataStoreEntry, List<ScanProvider.ScanOperation>> applicable) {
        var store = new SimpleObjectProperty<ShellStore>();
        var selected = new SimpleListProperty<ScanProvider.ScanOperation>(FXCollections.observableArrayList());

        var loading = new SimpleBooleanProperty();
        Platform.runLater(() -> {
            var stage = AppWindowHelper.sideWindow(
                    AppI18n.get("scanAlertTitle"),
                    window -> {
                        return new MultiStepComp() {

                            private final StackPane stackPane = new StackPane();

                            {
                                stackPane.getStyleClass().add("scan-list");
                            }

                            @Override
                            protected List<Entry> setup() {
                                return List.of(new Entry(AppI18n.observable("a"), new Step<CompStructure<?>>() {
                                    @Override
                                    public CompStructure<?> createBase() {
                                        var b = new OptionsBuilder()
                                                .name("scanAlertChoiceHeader")
                                                .description("scanAlertChoiceHeaderDescription")
                                                .addComp(new DataStoreChoiceComp<>(
                                                                DataStoreChoiceComp.Mode.OTHER,
                                                                null,
                                                                store,
                                                                ShellStore.class,
                                                                store1 -> true)
                                                        .disable(new SimpleBooleanProperty(initialStore != null)))
                                                .name("scanAlertHeader")
                                                .description("scanAlertHeaderDescription")
                                                .addComp(Comp.of(() -> stackPane).vgrow())
                                                .buildComp()
                                                .prefWidth(500)
                                                .prefHeight(600)
                                                .styleClass("window-content")
                                                .apply(struc -> {
                                                    VBox.setVgrow(((VBox) struc.get().getChildren().get(1)), ALWAYS);
                                                })
                                                .createStructure()
                                                .get();

                                        store.addListener((observable, oldValue, newValue) -> {
                                            selected.clear();
                                            stackPane.getChildren().clear();

                                            if (newValue == null) {
                                                return;
                                            }

                                            ThreadHelper.runAsync(() -> {
                                                BooleanScope.execute(loading, () -> {
                                                    var entry =
                                                            DataStorage.get().getStoreEntry(newValue);
                                                    var a = applicable.apply(entry);

                                                    Platform.runLater(() -> {
                                                        if (a == null) {
                                                            window.close();
                                                            return;
                                                        }

                                                        selected.setAll(a.stream()
                                                                .filter(
                                                                        scanOperation ->
                                                                                scanOperation.isDefaultSelected())
                                                                .toList());
                                                        var r = new ListSelectorComp<>(
                                                                        a,
                                                                        scanOperation ->
                                                                                AppI18n.get(scanOperation.getNameKey()),
                                                                        selected,
                                                                        a.size() > 3)
                                                                .createRegion();
                                                        stackPane.getChildren().add(r);
                                                    });
                                                });
                                            });
                                        });

                                        store.set(initialStore);
                                        return new SimpleCompStructure<>(b);
                                    }
                                }));
                            }

                            @Override
                            protected void finish() {
                                ThreadHelper.runAsync(() -> {
                                    if (store.get() == null) {
                                        return;
                                    }


                                    Platform.runLater(() -> {
                                        window.close();
                                    });

                                    BooleanScope.execute(loading, () -> {
                                        var entry = DataStorage.get().getStoreEntry(store.get());
                                        entry.setExpanded(true);

                                        for (var a : selected) {
                                            try {
                                                a.getScanner().run();
                                            } catch (Exception ex) {
                                                ErrorEvent.fromThrowable(ex).handle();
                                            }
                                        }
                                    });
                                });
                            }
                        };
                    },
                    false,
                    loading);
            stage.show();
        });
    }
}
