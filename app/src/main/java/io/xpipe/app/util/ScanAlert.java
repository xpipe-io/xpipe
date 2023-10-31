package io.xpipe.app.util;

import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.base.MultiStepComp;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.CompStructure;
import io.xpipe.app.fxcomps.SimpleCompStructure;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
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
        show(initial, (DataStoreEntry entry) -> {
            try (var sc = ((ShellStore) entry.getStore()).control().start()) {
                if (!sc.getShellDialect().isSupportedShell()) {
                    return null;
                }

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
            DataStoreEntry initialStore, Function<DataStoreEntry, List<ScanProvider.ScanOperation>> applicable) {
        var entry = new SimpleObjectProperty<DataStoreEntryRef<ShellStore>>();
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
                                return List.of(new Entry(AppI18n.observable("a"), new Step<>() {
                                    @Override
                                    public CompStructure<?> createBase() {
                                        var b = new OptionsBuilder()
                                                .name("scanAlertChoiceHeader")
                                                .description("scanAlertChoiceHeaderDescription")
                                                .addComp(new DataStoreChoiceComp<>(
                                                        DataStoreChoiceComp.Mode.OTHER,
                                                        null,
                                                        entry,
                                                        ShellStore.class,
                                                        store1 -> true,
                                                        StoreViewState.get().getAllConnectionsCategory()
                                                )
                                                                 .disable(new SimpleBooleanProperty(initialStore != null)))
                                                .name("scanAlertHeader")
                                                .description("scanAlertHeaderDescription")
                                                .addComp(Comp.of(() -> stackPane).vgrow())
                                                .buildComp()
                                                .prefWidth(500)
                                                .prefHeight(600)
                                                .styleClass("window-content")
                                                .apply(struc -> {
                                                    VBox.setVgrow(struc.get().getChildren().get(1), ALWAYS);
                                                })
                                                .createStructure()
                                                .get();

                                        entry.addListener((observable, oldValue, newValue) -> {
                                            selected.clear();
                                            stackPane.getChildren().clear();

                                            if (newValue == null) {
                                                return;
                                            }

                                            ThreadHelper.runAsync(() -> {
                                                BooleanScope.execute(loading, () -> {
                                                    var a = applicable.apply(entry.get().get());

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
                                                                a.size() > 3
                                                        )
                                                                .createRegion();
                                                        stackPane.getChildren().add(r);
                                                    });
                                                });
                                            });
                                        });

                                        entry.set(initialStore != null ? initialStore.ref() : null);
                                        return new SimpleCompStructure<>(b);
                                    }
                                }));
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

                                    BooleanScope.execute(loading, () -> {
                                        entry.get().get().setExpanded(true);

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
