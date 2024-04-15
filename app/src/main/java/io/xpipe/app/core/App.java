package io.xpipe.app.core;

import io.xpipe.app.comp.AppLayoutComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.core.process.OsType;
import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;

import java.awt.*;

@Getter
public class App extends Application {

    private static App APP;
    private Stage stage;

    public static App getApp() {
        return APP;
    }

    @Override
    @SneakyThrows
    public void start(Stage primaryStage) {
        TrackEvent.info("Application launched");
        APP = this;
        stage = primaryStage;
        stage.opacityProperty().bind(AppPrefs.get().windowOpacity());

        if (OsType.getLocal().equals(OsType.MACOS)) {
            Desktop.getDesktop().setPreferencesHandler(e -> {
                AppLayoutModel.get().selectSettings();
            });
        }

        if (OsType.getLocal().equals(OsType.LINUX)) {
            try {
                Toolkit xToolkit = Toolkit.getDefaultToolkit();
                java.lang.reflect.Field awtAppClassNameField =
                        xToolkit.getClass().getDeclaredField("awtAppClassName");
                awtAppClassNameField.setAccessible(true);
                awtAppClassNameField.set(xToolkit, "XPipe");
            } catch (Exception e) {
                ErrorEvent.fromThrowable(e).omit().handle();
            }
        }

        AppWindowHelper.addIcons(stage);
    }

    public void setupWindow() {
        var content = new AppLayoutComp();
        var t = LicenseProvider.get().licenseTitle();
        var u = XPipeDistributionType.get().getUpdateHandler().getPreparedUpdate();
        var titleBinding = Bindings.createStringBinding(
                () -> {
                    var base = String.format(
                            "XPipe %s (%s)", t.getValue(), AppProperties.get().getVersion());
                    var prefix = AppProperties.get().isStaging() ? "[Public Test Build, Not a proper release] " : "";
                    var suffix = u.getValue() != null
                            ? " " + AppI18n.get("updateReadyTitle", u.getValue().getVersion())
                            : "";
                    return prefix + base + suffix;
                },
                u,
                t,
                AppPrefs.get().language());

        var appWindow = AppMainWindow.init(stage);
        appWindow.getStage().titleProperty().bind(PlatformThread.sync(titleBinding));
        appWindow.initialize();
        appWindow.setContent(content);
        TrackEvent.info("Application window initialized");
        stage.setOnShown(event -> {
            focus();
        });
    }

    public void focus() {
        PlatformThread.runLaterIfNeeded(() -> {
            stage.requestFocus();
        });
    }
}
