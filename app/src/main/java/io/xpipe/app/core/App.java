package io.xpipe.app.core;

import io.xpipe.app.Main;
import io.xpipe.app.comp.AppLayoutComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.core.process.OsType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;

public class App extends Application {

    private static App APP;
    private Stage stage;

    public static App getApp() {
        return APP;
    }

    @Override
    public void start(Stage primaryStage) {
        TrackEvent.info("Application launched");
        APP = this;
        stage = primaryStage;

        // Set dock icon explicitly on mac
        // This is necessary in case XPipe was started through a script as it will have no icon otherwise
        if (OsType.getLocal().equals(OsType.MACOS) && AppProperties.get().isDeveloperMode() && AppLogs.get().isWriteToSysout()) {
            try {
                var iconUrl = Main.class.getResourceAsStream("resources/img/logo/logo_128x128.png");
                if (iconUrl != null) {
                    var awtIcon = ImageIO.read(iconUrl);
                    Taskbar.getTaskbar().setIconImage(awtIcon);
                }
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
            }
        }

        if (OsType.getLocal().equals(OsType.MACOS)) {
            Desktop.getDesktop().setPreferencesHandler(e -> {
                AppLayoutModel.get().selectSettings();
            });
        }

        if (OsType.getLocal().equals(OsType.LINUX)) {
            try {
                Toolkit xToolkit = Toolkit.getDefaultToolkit();
                java.lang.reflect.Field awtAppClassNameField = xToolkit.getClass().getDeclaredField("awtAppClassName");
                awtAppClassNameField.setAccessible(true);
                awtAppClassNameField.set(xToolkit, "XPipe");
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        }

        AppWindowHelper.addIcons(stage);
    }

    public void close() {
        Platform.runLater(() -> {
            Stage.getWindows().stream().toList().forEach(w -> w.hide());
            TrackEvent.debug("Closed main window");
        });
    }

    public void setupWindow() {
        var content = new AppLayoutComp();
        var titleBinding = Bindings.createStringBinding(
                () -> {
                    var base = String.format(
                            "XPipe Desktop (%s)", AppProperties.get().getVersion());
                    var prefix = AppProperties.get().isStaging() ? "[STAGING, Not a proper release] " : "";
                    var suffix = XPipeDistributionType.get()
                                            .getUpdateHandler()
                                            .getPreparedUpdate()
                                            .getValue()
                                    != null
                            ? String.format(
                                    " (Update to %s ready)",
                                    XPipeDistributionType.get()
                                            .getUpdateHandler()
                                            .getPreparedUpdate()
                                            .getValue()
                                            .getVersion())
                            : "";
                    return prefix + base + suffix;
                },
                XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate());

        var appWindow = AppMainWindow.init(stage);
        appWindow.getStage().titleProperty().bind(PlatformThread.sync(titleBinding));
        appWindow.initialize();
        appWindow.show();
        appWindow.setContent(content);
        TrackEvent.info("Application window initialized");
        stage.setOnShown(event -> {
            focus();
        });
    }

    public void focus() {
        PlatformThread.runLaterIfNeeded(() -> {
            stage.setAlwaysOnTop(true);
            stage.setAlwaysOnTop(false);
            stage.requestFocus();
        });
    }

    public Stage getStage() {
        return stage;
    }
}
