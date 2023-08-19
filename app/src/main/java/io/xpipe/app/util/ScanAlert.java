package io.xpipe.app.util;

import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.DataStoreChoiceComp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.ShellStore;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

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
        var busy = new SimpleBooleanProperty();
        var store = new SimpleObjectProperty<ShellStore>();
        var selected = new SimpleListProperty<ScanProvider.ScanOperation>(FXCollections.observableArrayList());
        AppWindowHelper.showAlert(
                alert -> {
                    alert.setAlertType(Alert.AlertType.NONE);
                    alert.setTitle(AppI18n.get("scanAlertTitle"));
                    alert.getButtonTypes().add(ButtonType.OK);
                    var content = new LoadingOverlayComp(
                                    new VerticalComp(List.<Comp<?>>of(
                                                    new LabelComp(AppI18n.get("scanAlertChoiceHeader"))
                                                            .apply(struc ->
                                                                    struc.get().setWrapText(true)),
                                                    new DataStoreChoiceComp<>(
                                                                    DataStoreChoiceComp.Mode.OTHER,
                                                                    null,
                                                                    store,
                                                                    ShellStore.class,
                                                                    store1 -> true)
                                                            .disable(new SimpleBooleanProperty(initialStore != null)),
                                                    new LabelComp(AppI18n.get("scanAlertHeader"))
                                                            .apply(struc ->
                                                                    struc.get().setWrapText(true))
                                                            .padding(new Insets(20, 0, 0, 0)),
                                                    Comp.of(() -> new Region())))
                                            .apply(struc -> struc.get().setSpacing(15))
                                            .styleClass("window-content"),
                                    busy)
                            .createRegion();
                    content.setPrefWidth(500);
                    content.setPrefHeight(550);
                    alert.getDialogPane().setContent(content);

                    // Custom behavior for ok button
                    var btOk = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
                    btOk.addEventFilter(ActionEvent.ACTION, event -> {
                        if (store.get() == null || busy.get()) {
                            event.consume();
                            return;
                        }

                        ThreadHelper.runAsync(() -> {
                            BusyProperty.execute(busy, () -> {
                                var entry = DataStorage.get().getStoreEntry(store.get());
                                entry.setExpanded(true);

                                for (var a : selected) {
                                    try {
                                        a.getScanner().run();
                                    } catch (Exception ex) {
                                        ErrorEvent.fromThrowable(ex).handle();
                                    }
                                }

                                Platform.runLater(() -> {
                                    alert.setResult(ButtonType.OK);
                                    alert.close();
                                });
                            });
                        });
                    });

                    store.addListener((observable, oldValue, newValue) -> {
                        selected.clear();
                        ((VBox) ((StackPane) alert.getDialogPane().getContent())
                                .getChildren()
                                .get(0))
                                .getChildren()
                                .set(3, new Region());

                        if (newValue == null) {
                            return;
                        }

                        ThreadHelper.runAsync(() -> {
                            BusyProperty.execute(busy, () -> {
                                var entry = DataStorage.get().getStoreEntry(newValue);
                                var a = applicable.apply(entry);

                                Platform.runLater(() -> {
                                    if (a == null) {
                                        alert.setResult(ButtonType.OK);
                                        alert.close();
                                        return;
                                    }

                                    selected.setAll(a.stream()
                                            .filter(scanOperation -> scanOperation.isDefaultSelected())
                                            .toList());
                                    var r = new ListSelectorComp<>(
                                                    a,
                                                    scanOperation -> AppI18n.get(scanOperation.getNameKey()),
                                                    selected,
                                                    a.size() > 3)
                                            .createRegion();
                                    ((VBox) ((StackPane) alert.getDialogPane().getContent())
                                                    .getChildren()
                                                    .get(0))
                                            .getChildren()
                                            .set(3, r);
                                });
                            });
                        });
                    });

                    store.set(initialStore);
                },
                busy,
                null);
    }
}
