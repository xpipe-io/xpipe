package io.xpipe.app.core;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.resources.AppImages;
import io.xpipe.app.resources.AppResources;
import io.xpipe.core.process.OsType;

import java.awt.*;
import java.io.IOException;
import java.net.URL;
import javax.imageio.ImageIO;

public class AppTrayIcon {

    private final SystemTray tray;
    private final TrayIcon trayIcon;

    public AppTrayIcon() {
        ensureSystemTraySupported();

        tray = SystemTray.getSystemTray();

        var image =
                switch (OsType.getLocal()) {
                    case OsType.Windows windows -> "img/logo/full/logo_16x16.png";
                    case OsType.Linux linux -> "img/logo/full/logo_24x24.png";
                    case OsType.MacOs macOs -> "img/logo/padded/logo_24x24.png";
                };
        var url = AppResources.getResourceURL(AppResources.XPIPE_MODULE, image).orElseThrow();

        PopupMenu popupMenu = new PopupMenu();
        this.trayIcon =
                new TrayIcon(loadImageFromURL(url), App.getApp().getStage().getTitle(), popupMenu);
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

    private static Image loadImageFromURL(URL iconImagePath) {
        try {
            return ImageIO.read(iconImagePath);
        } catch (IOException e) {
            ErrorEvent.fromThrowable(e).handle();
            return AppImages.toAwtImage(AppImages.DEFAULT_IMAGE);
        }
    }

    public static boolean isSupported() {
        return Desktop.isDesktopSupported() && SystemTray.isSupported();
    }

    public final TrayIcon getAwtTrayIcon() {
        return trayIcon;
    }

    private void ensureSystemTraySupported() {
        if (!SystemTray.isSupported()) {
            throw new UnsupportedOperationException(
                    "SystemTray icons are not " + "supported by the current desktop environment.");
        }
    }

    public void show() {
        EventQueue.invokeLater(() -> {
            try {
                tray.add(this.trayIcon);
            } catch (Exception e) {
                // This can sometimes fail on Linux
                ErrorEvent.fromThrowable("Unable to add TrayIcon", e).expected().handle();
            }
        });
    }

    public void hide() {
        EventQueue.invokeLater(() -> {
            tray.remove(trayIcon);
        });
    }

    public void showErrorMessage(String title, String message) {
        if (OsType.getLocal().equals(OsType.MACOS)) {
            showMacAlert(title, message, "Error");
        } else {
            EventQueue.invokeLater(() -> this.trayIcon.displayMessage(title, message, TrayIcon.MessageType.ERROR));
        }
    }

    private void showMacAlert(String subTitle, String message, String title) {
        String execute = String.format(
                "display notification \"%s\"" + " with title \"%s\"" + " subtitle \"%s\"",
                message != null ? message : "", title != null ? title : "", subTitle != null ? subTitle : "");
        try {
            Runtime.getRuntime().exec(new String[] {"osascript", "-e", execute});
        } catch (IOException e) {
            throw new UnsupportedOperationException("Cannot run osascript with given parameters.");
        }
    }
}
