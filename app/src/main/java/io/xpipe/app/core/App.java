package io.xpipe.app.core;

import io.xpipe.app.issue.TrackEvent;

import javafx.application.Application;
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
