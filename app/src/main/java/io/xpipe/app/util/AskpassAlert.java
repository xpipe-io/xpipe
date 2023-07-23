package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.fxcomps.impl.SecretFieldComp;
import io.xpipe.core.util.SecretValue;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.Alert;
import javafx.scene.layout.StackPane;

public class AskpassAlert {

    public static SecretValue query(String prompt) {
        var prop = new SimpleObjectProperty<SecretValue>();
        var r = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setTitle(AppI18n.get("askpassAlertTitle"));
                    alert.setHeaderText(prompt);
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);

                    var text = new SecretFieldComp(prop).createRegion();
                    alert.getDialogPane().setContent(new StackPane(text));
                })
                .filter(b -> b.getButtonData().isDefaultButton() && prop.getValue() != null)
                .map(t -> {
                    // AppCache.update(msg.getId(), prop.getValue());
                    return prop.getValue();
                })
                .orElse(null);
        return r;
    }
}
