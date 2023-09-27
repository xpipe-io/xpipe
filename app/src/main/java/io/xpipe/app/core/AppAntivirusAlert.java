package io.xpipe.app.core;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.OsType;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;

import java.nio.file.Files;
import java.util.Optional;

public class AppAntivirusAlert {

    public static Optional<String> detect() {
        var bitdefender = WindowsRegistry.readString(WindowsRegistry.HKEY_LOCAL_MACHINE,"SOFTWARE\\Bitdefender", "InstallDir");
        if (bitdefender.isPresent()) {
            return Optional.of("Bitdefender");
        }

        return Optional.empty();
    }

    public static void showIfNeeded() throws Throwable {
        // Only show this on first launch on windows
        if (OsType.getLocal() != OsType.WINDOWS || !AppState.get().isInitialLaunch()) {
            return;
        }

        var found = detect();
        if (found.isEmpty()) {
            return;
        }

        PlatformState.initPlatformOrThrow();
        AppStyle.init();
        AppImages.init();

        var a = AppWindowHelper.showBlockingAlert(alert -> {
            alert.setTitle(AppI18n.get("antivirusNoticeTitle"));
            alert.setAlertType(Alert.AlertType.NONE);

            AppResources.with(
                    AppResources.XPIPE_MODULE,
                    "misc/antivirus.md",
                    file -> {
                        var markdown = new MarkdownComp(Files.readString(file), s -> s.formatted(found.get(), found.get(), AppProperties.get().getVersion(), AppProperties.get().getVersion(), found.get())).prefWidth(550).prefHeight(600).createRegion();
                        alert.getDialogPane().setContent(markdown);
                        alert.getDialogPane().setPadding(new Insets(15));
                    });

            alert.getButtonTypes().add(new ButtonType(AppI18n.get("gotIt"), ButtonBar.ButtonData.OK_DONE));
        });
        a.filter(b -> b.getButtonData().isDefaultButton())
                .ifPresentOrElse(buttonType -> {}, () -> OperationMode.halt(1));
    }
}
