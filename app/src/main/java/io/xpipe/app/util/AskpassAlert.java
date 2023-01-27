package io.xpipe.app.util;

import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.core.util.SecretValue;
import io.xpipe.extension.I18n;
import javafx.scene.control.Alert;
import javafx.scene.control.PasswordField;

import java.util.concurrent.atomic.AtomicReference;

public class AskpassAlert {

    public static SecretValue query() {
        AtomicReference<SecretValue> password = new AtomicReference<>();
        var result = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    alert.setTitle(I18n.get("providePassword"));
                    alert.setHeaderText(I18n.get("queryPasswordDescription"));

                    var textField = new PasswordField();
                    textField.textProperty().addListener((c, o, n) -> {
                        password.set(new SecretValue(n));
                    });
                    alert.getDialogPane().setContent(textField);
                })
                .filter(buttonType -> buttonType.getButtonData().isDefaultButton());
        return result.isPresent() ? password.get() : null;
    }
}
