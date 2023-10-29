package io.xpipe.app.core;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;

import javax.imageio.ImageIO;
import java.awt.*;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;

public class AppTrayIcon {

    private boolean shown = false;

    private final SystemTray tray;

    private final TrayIcon trayIcon;

    private final PopupMenu popupMenu = new PopupMenu();

    public AppTrayIcon() {
        ensureSystemTraySupported();

        tray = SystemTray.getSystemTray();

        var image = switch (OsType.getLocal()) {
            case OsType.Windows windows -> "img/logo/logo_16x16.png";
            case OsType.Linux linux -> "img/logo/logo_24x24.png";
            case OsType.MacOs macOs -> "img/logo/logo_macos_tray_24x24.png";
        };
        var url = AppResources.getResourceURL(AppResources.XPIPE_MODULE, image).orElseThrow();

        this.trayIcon = new TrayIcon(loadImageFromURL(url), App.getApp().getStage().getTitle(), popupMenu);
        this.trayIcon.setToolTip("XPipe");
        this.trayIcon.setImageAutoSize(true);

        {
            var open = new MenuItem(AppI18n.get("open"));
            open.addActionListener(e -> {
                tray.remove(trayIcon);
                OperationMode.switchToAsync(OperationMode.GUI);
            });
            popupMenu.add(open);
        }

        {
            var quit = new MenuItem(AppI18n.get("quit"));
            quit.addActionListener(e -> {
                tray.remove(trayIcon);
                OperationMode.close();
            });
            popupMenu.add(quit);
        }

        trayIcon.addActionListener(e -> {
            if (OsType.getLocal() != OsType.MACOS) {
                tray.remove(trayIcon);
                OperationMode.switchToAsync(OperationMode.GUI);
            }
        });
    }

    public final TrayIcon getAwtTrayIcon() {
        return trayIcon;
    }

    private void ensureSystemTraySupported() {
        if (!SystemTray.isSupported()) {
            throw new UnsupportedOperationException(
                    "SystemTray icons are not "
                            + "supported by the current desktop environment.");
        }
    }

    private static Image loadImageFromURL(URL iconImagePath) {
        try {
            return ImageIO.read(iconImagePath);
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).handle();
            return AppImages.toAwtImage(AppImages.DEFAULT_IMAGE);
        }
    }

    public void show() {
        EventQueue.invokeLater(() -> {
            try {
                tray.add(this.trayIcon);
                shown = true;
                fixBackground();
            } catch (Exception e) {
                ErrorEvent.fromThrowable("Unable to add TrayIcon", e).handle();
            }
        });
    }

    private void fixBackground() {
        // Ugly fix to show a transparent background on Linux
        if (OsType.getLocal().equals(OsType.LINUX)) {
            EventQueue.invokeLater(() -> {
                try {
                    Field peerField;
                    peerField = TrayIcon.class.getDeclaredField("peer");
                    peerField.setAccessible(true);
                    var peer = peerField.get(this.trayIcon);

                    // If tray initialization fails, this can be null
                    if (peer == null) {
                        return;
                    }

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
        EventQueue.invokeLater(() -> {
            tray.remove(trayIcon);
            shown = false;
        });
    }

    public void showInfoMessage(String title, String message) {
        if (OsType.getLocal().equals(OsType.MACOS)) {
            showMacAlert(title, message,"Information");
        } else {
            EventQueue.invokeLater(() ->
                                           this.trayIcon.displayMessage(
                                                   title, message, TrayIcon.MessageType.INFO));
        }
    }

    public void showInfoMessage(String message) {
        this.showInfoMessage(null, message);
    }

    public void showWarningMessage(String title, String message) {
        if (OsType.getLocal().equals(OsType.MACOS)) {
            showMacAlert(title, message,"Warning");
        } else {
            EventQueue.invokeLater(() ->
                                           this.trayIcon.displayMessage(
                                                   title, message, TrayIcon.MessageType.WARNING));
        }
    }

    public void showWarningMessage(String message) {
        this.showWarningMessage(null, message);
    }

    public void showErrorMessage(String title, String message) {
        if (OsType.getLocal().equals(OsType.MACOS)) {
            showMacAlert(title, message,"Error");
        } else {
            EventQueue.invokeLater(() ->
                                           this.trayIcon.displayMessage(
                                                   title, message, TrayIcon.MessageType.ERROR));
        }
    }

    public void showErrorMessage(String message) {
        this.showErrorMessage(null, message);
    }

    public void showMessage(String title, String message) {
        if (OsType.getLocal().equals(OsType.MACOS)) {
            showMacAlert(title, message,"Message");
        } else {
            EventQueue.invokeLater(() ->
                                           this.trayIcon.displayMessage(
                                                   title, message, TrayIcon.MessageType.NONE));
        }
    }

    public void showMessage(String message) {
        this.showMessage(null, message);
    }

    public static boolean isSupported() {
        return Desktop.isDesktopSupported() && SystemTray.isSupported();
    }

    private void showMacAlert(String subTitle, String message, String title) {
        String execute = String.format(
                "display notification \"%s\""
                        + " with title \"%s\""
                        + " subtitle \"%s\"",
                message != null ? message : "",
                title != null ? title : "",
                subTitle != null ? subTitle : ""
        );
        try {
            Runtime.getRuntime()
                    .exec(new String[] { "osascript", "-e", execute });
        } catch (IOException e) {
            throw new UnsupportedOperationException(
                    "Cannot run osascript with given parameters.");
        }
    }
}

