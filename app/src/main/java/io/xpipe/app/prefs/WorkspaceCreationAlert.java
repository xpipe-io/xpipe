package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppFont;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.*;
import io.xpipe.core.process.OsType;
import io.xpipe.core.util.XPipeInstallation;

import javafx.beans.property.SimpleObjectProperty;

public class WorkspaceCreationAlert {

    public static void showAsync() {
        LicenseProvider.get().getFeature("workspaces").throwIfUnsupported();
        ThreadHelper.runFailableAsync(() -> {
            show();
        });
    }

    private static void show() throws Exception {
        var name = new SimpleObjectProperty<>("New workspace");
        var path = new SimpleObjectProperty<>(AppProperties.get().getDataDir());
        var content = new OptionsBuilder()
                .nameAndDescription("workspaceName")
                .addString(name)
                .nameAndDescription("workspacePath")
                .addPath(path)
                .buildComp()
                .prefWidth(500)
                .apply(struc -> AppFont.small(struc.get()));
        var modal = ModalOverlay.of("workspaceCreationAlertTitle", content);
        modal.addButton(ModalButton.ok(() -> {
            if (name.get() == null || path.get() == null) {
                return;
            }

            try {
                var shortcutName = (AppProperties.get().isStaging() ? "XPipe PTB" : "XPipe") + " (" + name.get() + ")";
                var file =
                        switch (OsType.getLocal()) {
                            case OsType.Windows w -> {
                                var exec = XPipeInstallation.getCurrentInstallationBasePath()
                                        .resolve(XPipeInstallation.getDaemonExecutablePath(w))
                                        .toString();
                                yield DesktopShortcuts.create(
                                        exec,
                                        "-Dio.xpipe.app.dataDir=\"" + path.get().toString()
                                                + "\" -Dio.xpipe.app.acceptEula=true",
                                        shortcutName);
                            }
                            default -> {
                                var exec = XPipeInstallation.getCurrentInstallationBasePath()
                                        .resolve(XPipeInstallation.getRelativeCliExecutablePath(OsType.getLocal()))
                                        .toString();
                                yield DesktopShortcuts.create(
                                        exec, "-d \"" + path.get().toString() + "\" --accept-eula", shortcutName);
                            }
                        };
                DesktopHelper.browseFileInDirectory(file);
                OperationMode.close();
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).handle();
            }
        }));
        modal.show();
    }
}
