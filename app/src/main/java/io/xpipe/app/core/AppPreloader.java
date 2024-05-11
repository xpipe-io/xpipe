package io.xpipe.app.core;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.process.OsType;
import javafx.application.Preloader;
import javafx.stage.Stage;
import lombok.Getter;
import lombok.SneakyThrows;

@Getter
public class AppPreloader extends Preloader {

    @Override
    @SneakyThrows
    public void start(Stage primaryStage) {
        if (OsType.getLocal() != OsType.LINUX) {
            return;
        }

        // Do it this way to prevent IDE inspections from complaining
        var c = Class.forName(
                ModuleLayer.boot().findModule("javafx.graphics").orElseThrow(), "com.sun.glass.ui.Application");
        var m = c.getDeclaredMethod("setName", String.class);
        m.invoke(c.getMethod("GetApplication").invoke(null), AppProperties.get().isStaging() ? "XPipe PTB" : "XPipe");
        TrackEvent.info("Application preloader run");
    }
}
