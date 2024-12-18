package io.xpipe.app.core;

import io.xpipe.app.comp.base.AppLayoutComp;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.PlatformThread;

import javafx.application.Application;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.stage.Stage;

import lombok.Getter;
import lombok.SneakyThrows;

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
        TrackEvent.debug("Application title bound");
        appWindow.initialize();
        appWindow.setContent(content);
        TrackEvent.info("Application window initialized");
    }

    public void focus() {
        PlatformThread.runLaterIfNeeded(() -> {
            stage.requestFocus();
        });
    }

    public ObservableDoubleValue displayScale() {
        if (getStage() == null) {
            return new SimpleDoubleProperty(1.0);
        }

        return getStage().outputScaleXProperty();
    }
}
