package io.xpipe.app.prefs;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppRestart;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.util.*;

import javafx.beans.property.SimpleObjectProperty;

import java.nio.file.Path;

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
                .apply(struc -> AppFontSizes.xs(struc));
        var modal = ModalOverlay.of("workspaceCreationAlertTitle", content);
        modal.addButton(ModalButton.ok(() -> {
            ThreadHelper.runAsync(() -> {
                if (name.get() == null || path.get() == null) {
                    return;
                }

                try {
                    var shortcutName = name.get();
                    var file = DesktopShortcuts.createOpen(
                            shortcutName,
                            "open -d \"" + path.get() + "\" --accept-eula",
                            "-Dio.xpipe.app.dataDir=\"" + path.get() + "\" -Dio.xpipe.app.acceptEula=true");
                    showConfirmModal(file, Path.of(path.get()));
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).handle();
                }
            });
        }));
        modal.show();
    }

    private static void showConfirmModal(Path shortcut, Path workspaceDir) {
        var modal = ModalOverlay.of("workspaceRestartTitle", AppDialog.dialogText(AppI18n.observable("workspaceRestartContent", shortcut)));
        modal.addButton(new ModalButton("browseShortcut", () -> {
            DesktopHelper.browseFileInDirectory(shortcut);
        }, false, false));
        modal.addButton(new ModalButton("restart", () -> {
            AppRestart.restart(workspaceDir);
        }, true, true));
        modal.show();
    }
}
