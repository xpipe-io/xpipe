package io.xpipe.app.core;

import io.xpipe.app.comp.base.AppLayoutComp;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.update.XPipeDistributionType;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.PlatformThread;

import javafx.application.Application;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableValue;
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
}
