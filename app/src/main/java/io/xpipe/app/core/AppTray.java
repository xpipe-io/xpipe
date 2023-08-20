package io.xpipe.app.core;

import com.dustinredmond.fxtrayicon.FXTrayIcon;
import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.ErrorHandler;
import io.xpipe.core.process.OsType;
import javafx.application.Platform;
import lombok.SneakyThrows;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Field;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;

public class AppTray {

    private static AppTray INSTANCE;
    private final FXTrayIcon icon;
    private final ErrorHandler errorHandler;
    private TrayIcon privateTrayIcon = null;

    @SneakyThrows
    private AppTray() {
        var image = switch (OsType.getLocal()) {
            case OsType.Windows windows -> "img/logo/logo_16x16.png";
            case OsType.Linux linux -> "img/logo/logo_24x24.png";
            case OsType.MacOs macOs -> "img/logo/logo_24x24.png";
        };
        var url = AppResources.getResourceURL(AppResources.XPIPE_MODULE, image).orElseThrow();

        var builder = new FXTrayIcon.Builder(App.getApp().getStage(), url)
                .menuItem(AppI18n.get("open"), e -> {
                    OperationMode.switchToAsync(OperationMode.GUI);
                });
        if (AppProperties.get().isDeveloperMode()) {
            builder.menuItem("Throw exception", e -> {
                        Platform.runLater(() -> {
                            throw new RuntimeException("This is a test exception");
                        });
                    })
                    .menuItem("Throw terminal exception", e -> {
                        try {
                            throw new RuntimeException("This is a terminal exception");
                        } catch (Exception ex) {
                            ErrorEvent.fromThrowable(ex).terminal(true).build().handle();
                        }
                    });
        }
        this.icon = builder.separator()
                .menuItem(AppI18n.get("quit"), e -> {
                    OperationMode.close();
                })
                .toolTip("XPipe")
                .build();
        this.errorHandler = new TrayErrorHandler();

        var tray = SystemTray.getSystemTray();
        var f = icon.getClass().getDeclaredField("trayIcon");
        f.setAccessible(true);
        privateTrayIcon = (TrayIcon) f.get(this.icon);
    }

    public static void init() {
        INSTANCE = new AppTray();
    }

    public static AppTray get() {
        return INSTANCE;
    }

    @SneakyThrows
    public void show() {
        icon.show();

        // Remove functionality to show stage when primary clicked and replace it with our own
        SwingUtilities.invokeLater(() -> {
            for (var l : Arrays.stream(privateTrayIcon.getActionListeners()).toList()) {
                privateTrayIcon.removeActionListener(l);
            }
            privateTrayIcon.addActionListener(e -> {
                if (OsType.getLocal() != OsType.MACOS) {
                    OperationMode.switchToAsync(OperationMode.GUI);
                }
            });
        });

        // Ugly fix to show a transparent background on Linux
        if (OsType.getLocal().equals(OsType.LINUX)) {
            SwingUtilities.invokeLater(() -> {
                try {
                    Field peerField = null;
                    peerField = TrayIcon.class.getDeclaredField("peer");
                    peerField.setAccessible(true);
                    var peer = peerField.get(this.privateTrayIcon);

                    var canvasField = peer.getClass().getDeclaredField("canvas");
                    canvasField.setAccessible(true);
                    Component canvas = (Component) canvasField.get(peer);
                    canvas.setBackground(new Color(0, 0, 0, 0));

                    var frameField = peer.getClass().getDeclaredField("eframe");
                    frameField.setAccessible(true);
                    Frame frame = (Frame) frameField.get(peer);
                    frame.setTitle("XPipe");
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).omit().handle();
                }
            });
        }
    }

    public void hide() {
        // Ugly fix to prevent platform exit in icon.hide()
        try {
            var tray = SystemTray.getSystemTray();
            EventQueue.invokeLater(() -> {
                tray.remove(privateTrayIcon);
            });
        } catch (Exception ex) {
            ErrorEvent.fromThrowable(ex).handle();
        }
    }

    public ErrorHandler getErrorHandler() {
        return errorHandler;
    }

    private class TrayErrorHandler implements ErrorHandler {

        private Instant lastErrorShown = Instant.MIN;

        @Override
        public void handle(ErrorEvent event) {
            if (event.isOmitted()) {
                return;
            }

            var title = AppI18n.get(event.isTerminal() ? "terminalErrorOccured" : "errorOccured");
            var desc = event.getDescription();
            if (desc == null && event.getThrowable() != null) {
                var tName = event.getThrowable().getClass().getSimpleName();
                desc = AppI18n.get("errorTypeOccured", tName);
            }
            if (desc == null) {
                desc = AppI18n.get("errorNoDetail");
            }

            String finalDesc = desc;
            Platform.runLater(() -> {
                if (Duration.between(lastErrorShown, Instant.now()).getSeconds() < 10) {
                    return;
                }

                lastErrorShown = Instant.now();
                AppTray.this.icon.showErrorMessage(title, finalDesc);
            });
        }
    }
}
