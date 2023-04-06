package io.xpipe.app.util;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppWindowHelper;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.update.AppDownloads;
import io.xpipe.app.update.AppInstaller;
import io.xpipe.core.impl.FileNames;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.util.ModuleHelper;
import io.xpipe.core.util.ProxyManagerProvider;
import io.xpipe.core.util.XPipeInstallation;
import javafx.scene.control.Alert;

import java.util.Optional;

public class ProxyManagerProviderImpl extends ProxyManagerProvider {

    private static boolean showAlert() {
        var okay = AppWindowHelper.showBlockingAlert(alert -> {
                    alert.setAlertType(Alert.AlertType.CONFIRMATION);
                    alert.setTitle(AppI18n.get("connectorInstallationTitle"));
                    alert.setHeaderText(AppI18n.get("connectorInstallationHeader"));
                    alert.getDialogPane()
                            .setContent(AppWindowHelper.alertContentText(AppI18n.get("connectorInstallationContent")));
                })
                .filter(buttonType -> buttonType.getButtonData().isDefaultButton())
                .isPresent();
        return okay;
    }

    @Override
    public Optional<String> checkCompatibility(ShellControl s) throws Exception {
        var version = ModuleHelper.isImage() ? AppProperties.get().getVersion() : AppDownloads.getLatestVersion();

        if (AppPrefs.get().developerDisableConnectorInstallationVersionCheck().get()) {
            return Optional.of(AppI18n.get("versionCheckOverride"));
        }

        var defaultInstallationExecutable = FileNames.join(
                XPipeInstallation.getDefaultInstallationBasePath(s, false),
                XPipeInstallation.getDaemonExecutablePath(s.getOsType()));
        if (!s.getShellDialect().createFileExistsCommand(s, defaultInstallationExecutable).executeAndCheck()) {
            return Optional.of(AppI18n.get("noInstallationFound"));
        }

        var installationVersion = XPipeInstallation.queryInstallationVersion(s, defaultInstallationExecutable);
        if (!version.equals(installationVersion)) {
            return Optional.of(AppI18n.get("installationVersionMismatch", version, installationVersion));
        }

        return Optional.empty();
    }

    @Override
    public boolean setup(ShellControl s) throws Exception {
        var message = checkCompatibility(s);
        if (message.isPresent()) {
            if (showAlert()) {
                var version =
                        ModuleHelper.isImage() ? AppProperties.get().getVersion() : AppDownloads.getLatestVersion();
                AppInstaller.installOnRemoteMachine(s, version);
                return true;
            }

            return false;
        } else {
            return true;
        }
    }
}
