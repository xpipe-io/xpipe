package io.xpipe.app.util;

import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.comp.base.LoadingOverlayComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.store.ShellStore;
import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public class ScanAlert {

    public static void showAsync(DataStoreEntry entry) {
        ThreadHelper.runAsync(() -> {
            if (entry.getStore() instanceof ShellStore) {
                showForShellStore(entry);
            } else {
                showForOtherStore(entry);
            }
        });
    }

    private static void showForOtherStore(DataStoreEntry entry) {
        show(entry, () -> {
            var providers = ScanProvider.getAll();
            var applicable = providers.stream()
                    .map(scanProvider -> scanProvider.create(entry.getStore()))
                    .filter(scanOperation -> scanOperation != null)
                    .toList();
            return applicable;
        });
    }

    private static void showForShellStore(DataStoreEntry entry) {
        show(entry, () -> {
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

    private static void show(DataStoreEntry entry, Supplier<List<ScanProvider.ScanOperation>> applicable) {
        var busy = new SimpleBooleanProperty();
        var selected = new SimpleListProperty<ScanProvider.ScanOperation>(FXCollections.observableArrayList());
        AppWindowHelper.showAlert(
                alert -> {
                    alert.setAlertType(Alert.AlertType.NONE);
                    alert.setTitle(AppI18n.get("scanAlertTitle"));
                    alert.getButtonTypes().add(ButtonType.OK);
                    var content = new LoadingOverlayComp(
                                    new VerticalComp(List.<Comp<?>>of(
                                                    new LabelComp(AppI18n.get("scanAlertHeader"))
                                                            .apply(struc ->
                                                                    struc.get().setWrapText(true)),
                                                    Comp.of(() -> new Region())))
                                            .apply(struc -> struc.get().setSpacing(15))
                                            .styleClass("window-content"),
                                    busy)
                            .createRegion();
                    content.setPrefWidth(500);
                    content.setPrefHeight(300);
                    alert.getDialogPane().setContent(content);

                    // Custom behavior for ok button
                    var btOk = (Button) alert.getDialogPane().lookupButton(ButtonType.OK);
                    btOk.addEventFilter(ActionEvent.ACTION, event -> {
                        ThreadHelper.runAsync(() -> {
                            BusyProperty.execute(busy, () -> {
                                for (var a : selected) {
                                    try {
                                        a.getScanner().run();
                                    } catch (Exception ex) {
                                        ErrorEvent.fromThrowable(ex).handle();
                                    }
                                }

                                entry.setExpanded(true);

                                Platform.runLater(() -> {
                                    alert.setResult(ButtonType.OK);
                                    alert.close();
                                });
                            });
                        });
                    });

                    // Asynchronous loading of content
                    alert.setOnShown(event -> {
                        ThreadHelper.runAsync(() -> {
                            BusyProperty.execute(busy, () -> {
                                var a = applicable.get();

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
                                                    selected)
                                            .createRegion();
                                    ((VBox) ((StackPane) alert.getDialogPane().getContent())
                                                    .getChildren()
                                                    .get(0))
                                            .getChildren()
                                            .set(1, r);
                                });
                            });
                        });
                    });
                },
                busy,
                null);
    }
}
