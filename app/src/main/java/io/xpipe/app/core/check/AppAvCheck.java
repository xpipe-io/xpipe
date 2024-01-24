package io.xpipe.app.core.check;

import io.xpipe.app.comp.base.MarkdownComp;
import io.xpipe.app.core.*;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.util.PlatformState;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.OsType;
import javafx.geometry.Insets;
import javafx.scene.control.Alert;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import lombok.Getter;

import java.nio.file.Files;
import java.util.Optional;

public class AppAvCheck {

    @Getter
    public static enum AvType {

        BITDEFENDER("Bitdefender") {
            @Override
            public String getDescription() {
                return "Bitdefender sometimes isolates XPipe and some shell programs, effectively making it unusable.";
            }

            @Override
            public boolean isActive() {
                return WindowsRegistry.exists(WindowsRegistry.HKEY_LOCAL_MACHINE,"SOFTWARE\\Bitdefender", "InstallDir");
            }
        },
        MALWAREBYTES("Malwarebytes") {
            @Override
            public String getDescription() {
                return "The free Malwarebytes version performs less invasive scans, so it shouldn't be a problem. If you are running the paid Malwarebytes Pro version, you will have access to the `Exploit Protection` under the `Real-time Protection` mode. When this setting is active, any shell access is slowed down, resulting in XPipe becoming very slow.";
            }

            @Override
            public boolean isActive() {
                return WindowsRegistry.exists(WindowsRegistry.HKEY_LOCAL_MACHINE,"SOFTWARE\\Malwarebytes", "id");
            }
        },
        MCAFEE("McAfee") {
            @Override
            public String getDescription() {
                return "McAfee slows down XPipe considerably. It also sometimes preemptively disables some Win32 commands that XPipe depends on, leading to errors.";
            }

            @Override
            public boolean isActive() {
                return WindowsRegistry.exists(WindowsRegistry.HKEY_LOCAL_MACHINE,"SOFTWARE\\McAfee", "mi");
            }
        };

        private final String name;

        AvType(String name) {
            this.name = name;
        }

        public abstract String getDescription();

        public abstract boolean isActive();
    }

    private static Optional<AvType> detect() {
        for (AvType value : AvType.values()) {
            if (value.isActive()) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    public static void check() throws Throwable {
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
                        var markdown = new MarkdownComp(Files.readString(file), s -> {
                            var t = found.get();
                            return s.formatted(t.getName(), t.getName(), t.getDescription(), AppProperties.get().getVersion(), AppProperties.get().getVersion(), t.getName());
                        }).prefWidth(550).prefHeight(600).createRegion();
                        alert.getDialogPane().setContent(markdown);
                        alert.getDialogPane().setPadding(new Insets(15));
                    });

            alert.getButtonTypes().add(new ButtonType(AppI18n.get("gotIt"), ButtonBar.ButtonData.OK_DONE));
        });
        a.filter(b -> b.getButtonData().isDefaultButton())
                .ifPresentOrElse(buttonType -> {}, () -> OperationMode.halt(1));
    }
}
