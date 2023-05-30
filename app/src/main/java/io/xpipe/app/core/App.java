package io.xpipe.app.core;

import io.xpipe.app.Main;
import io.xpipe.app.comp.AppLayoutComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.PlatformState;
import io.xpipe.core.process.OsType;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.scene.image.Image;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.*;

public class App extends Application {

    private static App APP;
    private Stage stage;
    private Image icon;

    public static App getApp() {
        return APP;
    }

    @Override
    public void start(Stage primaryStage) {
        TrackEvent.info("Application launched");
        APP = this;
        PlatformState.setCurrent(PlatformState.RUNNING);
        stage = primaryStage;
        icon = AppImages.image("logo.png");

        // Set dock icon explicitly on mac
        // This is necessary in case XPipe was started through a script as it will have no icon otherwise
        if (OsType.getLocal().equals(OsType.MACOS)) {
            try {
                var iconUrl = Main.class.getResourceAsStream("resources/img/logo.png");
                if (iconUrl != null) {
                    var awtIcon = ImageIO.read(iconUrl);
                    Taskbar.getTaskbar().setIconImage(awtIcon);
                }
            } catch (Exception ex) {
                ErrorEvent.fromThrowable(ex).omitted(true).build().handle();
            }
        }

        primaryStage.getIcons().clear();
        primaryStage.getIcons().add(icon);
        Platform.setImplicitExit(false);
    }

    public void close() {
        Platform.runLater(() -> {
            stage.hide();
            TrackEvent.debug("Closed main window");
        });
    }

    public void setupWindow() {
        var content = new AppLayoutComp();
        var titleBinding = Bindings.createStringBinding(
                () -> {
                    var base = String.format(
                            "XPipe Desktop (%s)", AppProperties.get().getVersion());
                    var prefix = AppProperties.get().isStaging() ? "[STAGE] " : "";
                    var suffix = XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate().getValue() != null
                            ? String.format(
                            " (Update to %s ready)",
                            XPipeDistributionType.get().getUpdateHandler()
                                            .getPreparedUpdate()
                                            .getValue()
                                            .getVersion())
                            : "";
                    return prefix + base + suffix;
                },
                XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate());

        var appWindow = new AppMainWindow(stage);
        appWindow.getStage().titleProperty().bind(PlatformThread.sync(titleBinding));
        appWindow.initialize();
        appWindow.setContent(content);
        TrackEvent.info("Application window initialized");
        stage.setOnShown(event -> {
            focus();
        });
        appWindow.show();

        // For demo purposes
        //        if (true) {
        //            stage.setX(310);
        //            stage.setY(178);
        //            stage.setWidth(1300);
        //            stage.setHeight(730);
        //        }
    }

    public void focus() {
        PlatformThread.runLaterIfNeeded(() -> {
            stage.setAlwaysOnTop(true);
            stage.setAlwaysOnTop(false);
            stage.requestFocus();
        });
    }

    public Image getIcon() {
        return icon;
    }

    public Stage getStage() {
        return stage;
    }
}
