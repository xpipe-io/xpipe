package io.xpipe.app.util;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.impl.LabelComp;
import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.SecretValue;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;

public class LockChangeAlert {

    public static void show() {
        var prop1 = new SimpleObjectProperty<SecretValue>();
        var prop2 = new SimpleObjectProperty<SecretValue>();
        AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("lockCreationAlertTitle"));
                    alert.setHeaderText(AppI18n.get("lockCreationAlertHeader"));
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);

                    var label1 = new LabelComp(AppI18n.observable("password")).createRegion();
                    var p1 = new SecretFieldComp(prop1).createRegion();
                    p1.setStyle("-fx-border-width: 1px");

                    var label2 = new LabelComp(AppI18n.observable("repeatPassword")).createRegion();
                    var p2 = new SecretFieldComp(prop2).createRegion();
                    p1.setStyle("-fx-border-width: 1px");

                    var content = new VBox(label1, p1, new Spacer(15), label2, p2);
                    content.setSpacing(5);
                    alert.getDialogPane().setContent(content);
                })
                .filter(b -> b.getButtonData().isDefaultButton() && (prop1.getValue() != null && prop2.getValue() != null && prop1.getValue().equals(prop2.getValue())) || (prop1.getValue() == null && prop2.getValue() == null))
                .ifPresent(t -> {
                    AppPrefs.get().changeLock(prop1.getValue());
                });
    }
}
