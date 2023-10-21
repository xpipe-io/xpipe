package io.xpipe.app.core;

import io.xpipe.app.core.mode.OperationMode;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.core.process.OsType;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.IOException;
import java.lang.reflect.Field;
import java.net.URL;

public class AppTrayIcon {

    private static final Integer winScale = 16;
    private static final Integer coreScale = 22;
    private boolean shown = false;
    private ActionListener exitMenuItemActionListener;


    /**
     * The default AWT SystemTray
     */
    private final SystemTray tray;

    /**
     * The parent Stage of the FXTrayIcon
     */
    private Stage parentStage;

    /**
     * The application's title, to be used
     * as default tooltip text for the FXTrayIcon
     */
    private String appTitle;

    /**
     * The AWT TrayIcon managed by FXTrayIcon
     */
    private final TrayIcon trayIcon;

    /**
     * The AWT PopupMenu managed by FXTrayIcon
     */
    private final PopupMenu popupMenu = new PopupMenu();

    /**
     * Creates a {@code MouseListener} whose
     * single-click action performs the passed
     * JavaFX EventHandler
     * @param e A JavaFX event to be performed
     * @return A MouseListener fired by single-click
     */
    private MouseListener getPrimaryClickListener(EventHandler<ActionEvent> e) {
        return new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent me) {
                Platform.runLater(() -> e.handle(new ActionEvent()));
            }

            @Override
            public void mousePressed(MouseEvent ignored) { }
            @Override
            public void mouseReleased(MouseEvent ignored) { }
            @Override
            public void mouseEntered(MouseEvent ignored) { }
            @Override
            public void mouseExited(MouseEvent ignored) { }
        };
    }

    public AppTrayIcon() {
        ensureSystemTraySupported();

        tray = SystemTray.getSystemTray();

        var image = switch (OsType.getLocal()) {
            case OsType.Windows windows -> "img/logo/logo_16x16.png";
            case OsType.Linux linux -> "img/logo/logo_24x24.png";
            case OsType.MacOs macOs -> "img/logo/logo_24x24.png";
        };
        var url = AppResources.getResourceURL(AppResources.XPIPE_MODULE, image).orElseThrow();

        this.trayIcon = new TrayIcon(loadImageFromURL(url), App.getApp().getStage().getTitle(), popupMenu);
        this.trayIcon.setImageAutoSize(false);
        this.trayIcon.setToolTip("XPipe");

        {
            var open = new MenuItem(AppI18n.get("open"));
            open.addActionListener(e -> {
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
                OperationMode.switchToAsync(OperationMode.GUI);
            }
        });
    }

    /**
     * Gets the nested AWT {@link TrayIcon}. This is intended for extended
     * instances of FXTrayIcon which require the access to implement
     * custom features.
     * @return The nest trayIcon within this instance of FXTrayIcon.
     */
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

    /**
     * Adds the FXTrayIcon to the system tray.
     * This will add the TrayIcon with the image initialized in the
     * {@code FXTrayIcon}'s constructor. By default, an empty popup
     * menu is shown.
     * By default, {@code javafx.application.Platform.setImplicitExit(false)}
     * will be called. This will allow the application to continue running
     * and show the tray icon after no more JavaFX Stages are visible. If
     * this is not the behavior that you intend, call {@code setImplicitExit}
     * to true after calling {@code show()}.
     */
    public void show() {
        SwingUtilities.invokeLater(() -> {
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
            SwingUtilities.invokeLater(() -> {
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

    /**
     * Displays an info popup message near the tray icon.
     * <p>NOTE: Some systems do not support this.</p>
     * @param title The caption (header) text
     * @param message The message content text
     */
    public void showInfoMessage(String title, String message) {
        if (OsType.getLocal().equals(OsType.MACOS)) {
            showMacAlert(title, message,"Information");
        } else {
            EventQueue.invokeLater(() ->
                                           this.trayIcon.displayMessage(
                                                   title, message, TrayIcon.MessageType.INFO));
        }
    }

    /**
     * Displays an info popup message near the tray icon.
     * <p>NOTE: Some systems do not support this.</p>
     * @param message The message content text
     */
    public void showInfoMessage(String message) {
        this.showInfoMessage(null, message);
    }

    /**
     * Displays a warning popup message near the tray icon.
     * <p>NOTE: Some systems do not support this.</p>
     * @param title The caption (header) text
     * @param message The message content text
     */
    public void showWarningMessage(String title, String message) {
        if (OsType.getLocal().equals(OsType.MACOS)) {
            showMacAlert(title, message,"Warning");
        } else {
            EventQueue.invokeLater(() ->
                                           this.trayIcon.displayMessage(
                                                   title, message, TrayIcon.MessageType.WARNING));
        }
    }

    /**
     * Displays a warning popup message near the tray icon.
     * <p>NOTE: Some systems do not support this.</p>
     * @param message The message content text
     */
    public void showWarningMessage(String message) {
        this.showWarningMessage(null, message);
    }

    /**
     * Displays an error popup message near the tray icon.
     * <p>NOTE: Some systems do not support this.</p>
     * @param title The caption (header) text
     * @param message The message content text
     */
    public void showErrorMessage(String title, String message) {
        if (OsType.getLocal().equals(OsType.MACOS)) {
            showMacAlert(title, message,"Error");
        } else {
            EventQueue.invokeLater(() ->
                                           this.trayIcon.displayMessage(
                                                   title, message, TrayIcon.MessageType.ERROR));
        }
    }

    /**
     * Displays an error popup message near the tray icon.
     * <p>NOTE: Some systems do not support this.</p>
     * @param message The message content text
     */
    public void showErrorMessage(String message) {
        this.showErrorMessage(null, message);
    }

    /**
     * Displays a popup message near the tray icon.
     * Some systems will display FXTrayIcon's image on this popup.
     * <p>NOTE: Some systems do not support this.</p>
     * @param title The caption (header) text
     * @param message The message content text
     */
    public void showMessage(String title, String message) {
        if (OsType.getLocal().equals(OsType.MACOS)) {
            showMacAlert(title, message,"Message");
        } else {
            EventQueue.invokeLater(() ->
                                           this.trayIcon.displayMessage(
                                                   title, message, TrayIcon.MessageType.NONE));
        }
    }

    /**
     * Displays a popup message near the tray icon.
     * Some systems will display FXTrayIcon's image on this popup.
     * <p>NOTE: Some systems do not support this.</p>
     * @param message The message content text
     */
    public void showMessage(String message) {
        this.showMessage(null, message);
    }

    /**
     * Checks whether the system tray icon is supported on the
     * current platform, or not.
     * Just because the system tray is supported, does not mean that the
     * current platform implements all system tray functionality.
     * This will always return true on Windows or MacOS. Check the
     * specific desktop environment for AppIndicator support when
     * calling this on *nix platforms.
     * @return false if the system tray is not supported, true if any
     *          part of the system tray functionality is supported.
     */
    public static boolean isSupported() {
        return Desktop.isDesktopSupported() && SystemTray.isSupported();
    }

    /**
     * Displays a sliding info message. Behavior is similar to Windows, but without AWT
     * @param subTitle The message caption
     * @param message The message text
     * @param title The message title
     */
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

