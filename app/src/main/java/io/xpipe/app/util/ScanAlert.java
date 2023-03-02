package io.xpipe.app.util;

import io.xpipe.app.comp.base.ListSelectorComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.ext.ScanProvider;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.impl.VerticalComp;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleListProperty;
import javafx.collections.FXCollections;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;

import java.util.ArrayList;
import java.util.List;

public class ScanAlert {

    public static void showIfNeeded(DataStore store) {
        var providers = ScanProvider.getAll();
        var applicable = providers.stream()
                .map(scanProvider -> scanProvider.create(store))
                .filter(scanOperation -> scanOperation != null)
                .toList();
        if (applicable.size() == 0) {
            return;
        }

        var selected = new SimpleListProperty<ScanProvider.ScanOperation>(
                FXCollections.observableList(new ArrayList<>(applicable)));
        var busy = new SimpleBooleanProperty();
        AppWindowHelper.showAlert(
                alert -> {
                    alert.setAlertType(Alert.AlertType.NONE);
                    alert.setTitle(AppI18n.get("scanAlertTitle"));
                    alert.setWidth(300);
                    var content = new VerticalComp(List.of(
                                    new LabelComp(AppI18n.get("scanAlertHeader")).apply(struc -> struc.get().setWrapText(true)),
                                    new ListSelectorComp<>(
                                            applicable, scanOperation -> AppI18n.get(scanOperation.getNameKey()), selected)))
                            .apply(struc -> struc.get().setSpacing(15))
                            .styleClass("window-content")
                            .createRegion();
                    alert.getButtonTypes().add(ButtonType.OK);
                    alert.getDialogPane().setContent(content);
                },
                busy,
                buttonType -> {
                    if (buttonType.isPresent()
                            && buttonType.get().getButtonData().isDefaultButton()) {
                        try (var ignored = new BusyProperty(busy)) {
                            for (var a : applicable) {
                                a.getScanner().run();
                            }
                        } catch (Exception ex) {
                            ErrorEvent.fromThrowable(ex).handle();
                        }
                    }
                });
    }
}
