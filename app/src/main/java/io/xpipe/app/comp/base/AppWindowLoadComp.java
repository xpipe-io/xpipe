package io.xpipe.app.comp.base;

import atlantafx.base.controls.RingProgressIndicator;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.prefs.AppPrefs;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class AppWindowLoadComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var loading = new RingProgressIndicator(-1, false);
        loading.visibleProperty().bind(Bindings.not(AppPrefs.get().performanceMode()));
        loading.setPrefWidth(60);
        loading.setPrefHeight(60);

        var pane = new StackPane(loading);
        pane.getStyleClass().add("background");
        return pane;
    }
}
