package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppWindowHelper;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.util.ProxyManagerProvider;

import javafx.scene.control.Alert;

import java.util.Optional;

public class ProxyManagerProviderImpl extends ProxyManagerProvider {

    private static boolean showAlert() {
        return AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    alert.setTitle(AppI18n.get("connectorInstallationTitle"));
                    alert.setHeaderText(AppI18n.get("connectorInstallationHeader"));
                    alert.getDialogPane()
                            .setContent(AppWindowHelper.alertContentText(AppI18n.get("connectorInstallationContent")));
                })
                .filter(buttonType -> buttonType.getButtonData().isDefaultButton())
                .isPresent();
    }

    @Override
    public Optional<String> checkCompatibility(ShellControl s) {
        return Optional.empty();
    }

    @Override
    public boolean setup(ShellControl s) {
        return true;
    }
}
