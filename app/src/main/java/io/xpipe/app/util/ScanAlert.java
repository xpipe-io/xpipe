package io.xpipe.app.util;

import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.ArrayList;
import java.util.List;

public class ScanAlert {

    public static void showAsync(DataStore store, boolean automatic) {
        ThreadHelper.runAsync(() -> {
            if (store instanceof ShellStore) {
                showForShellStore(store.asNeeded(), automatic);
            } else {
                showForOtherStore(store, automatic);
            }
        });
    }

    private  static void showForOtherStore(DataStore store, boolean automatic) {
        var providers = ScanProvider.getAll();
        var applicable = providers.stream()
                .map(scanProvider -> scanProvider.create(store, automatic))
                .filter(scanOperation -> scanOperation != null)
                .toList();
        showIfNeeded(applicable);
    }

    private  static void showForShellStore(ShellStore store, boolean automatic) {
        try (var sc = store.control().start()) {
            var providers = ScanProvider.getAll();
            var applicable = new ArrayList<ScanProvider.ScanOperation>();
            for (ScanProvider scanProvider : providers) {
                ScanProvider.ScanOperation operation = scanProvider.create(store, sc, automatic);
                if (operation != null) {
                    applicable.add(operation);
                }
            }
            showIfNeeded(applicable);
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex);
        }
    }

    private  static void showIfNeeded(List<ScanProvider.ScanOperation> applicable) {
        if (applicable.size() == 0) {
            return;
        }

        var selected = new SimpleListProperty<ScanProvider.ScanOperation>(
                FXCollections.observableList(new ArrayList<>(applicable.stream()
                        .filter(scanOperation -> scanOperation.isDefaultSelected())
                        .toList())));
        var busy = new SimpleBooleanProperty();
        AppWindowHelper.showAlert(
                alert -> {
                    alert.setAlertType(Alert.AlertType.NONE);
                    alert.setTitle(AppI18n.get("scanAlertTitle"));
                    var content = new VerticalComp(List.of(
                                    new LabelComp(AppI18n.get("scanAlertHeader"))
                                            .apply(struc -> struc.get().setWrapText(true)),
                                    new ListSelectorComp<>(
                                            applicable,
                                            scanOperation -> AppI18n.get(scanOperation.getNameKey()),
                                            selected)))
                            .apply(struc -> struc.get().setSpacing(15))
                            .styleClass("window-content")
                            .createRegion();
                    content.setPrefWidth(500);
                    alert.getButtonTypes().add(ButtonType.OK);
                    alert.getDialogPane().setContent(content);
                },
                busy,
                buttonType -> {
                    if (buttonType.isPresent()
                            && buttonType.get().getButtonData().isDefaultButton()) {
                        for (var a : selected) {
                            try (var ignored = new BusyProperty(busy)) {
                                a.getScanner().run();
                            } catch (Exception ex) {
                                ErrorEvent.fromThrowable(ex).handle();
                            }
                        }
                    }
                });
    }
}
