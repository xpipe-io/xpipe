package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.util.*;
import io.xpipe.core.OsType;

import javafx.beans.property.SimpleObjectProperty;

public class WorkspaceCreationDialog {

    public static void showAsync() {
        LicenseProvider.get().getFeature("workspaces").throwIfUnsupported();
        ThreadHelper.runFailableAsync(() -> {
            show();
        });
    }

    private static void show() {
        var base = AppProperties.get().getDataDir().toString();
        var name = new SimpleObjectProperty<>("new-workspace");
        var path = new SimpleObjectProperty<>(base + "-new-workspace");
        name.subscribe((v) -> {
            if (v != null && path.get() != null && path.get().startsWith(base)) {
                var newPath = path.get().substring(0, base.length()) + "-"
                        + v.replaceAll(" ", "-").toLowerCase();
                path.set(newPath);
            }
        });
        var content = new OptionsBuilder()
                .nameAndDescription("workspaceName")
                .addString(name)
                .nameAndDescription("workspacePath")
                .addString(path)
                .buildComp()
                .prefWidth(500)
                .apply(struc -> AppFontSizes.xs(struc.get()));
        var modal = ModalOverlay.of("workspaceCreationAlertTitle", content);
        modal.addButton(ModalButton.ok(() -> {
            ThreadHelper.runAsync(() -> {
                if (name.get() == null || path.get() == null) {
                    return;
                }

                try {
                    var shortcutName = name.get();
                    var file =
                            switch (OsType.getLocal()) {
                                case OsType.Windows ignored -> {
                                    var exec = AppInstallation.ofCurrent()
                                            .getDaemonExecutablePath()
                                            .toString();
                                    yield DesktopShortcuts.create(
                                            exec,
                                            "-Dio.xpipe.app.dataDir=\""
                                                    + path.get().toString() + "\" -Dio.xpipe.app.acceptEula=true",
                                            shortcutName);
                                }
                                default -> {
                                    var exec = AppInstallation.ofCurrent()
                                            .getCliExecutablePath()
                                            .toString();
                                    yield DesktopShortcuts.create(
                                            exec,
                                            "open -d \"" + path.get().toString() + "\" --accept-eula",
                                            shortcutName);
                                }
                            };
                    DesktopHelper.browseFileInDirectory(file);
                    OperationMode.close();
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).handle();
                }
            });
        }));
        modal.show();
    }
}
