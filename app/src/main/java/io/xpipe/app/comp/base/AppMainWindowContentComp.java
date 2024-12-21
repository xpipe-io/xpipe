package io.xpipe.app.comp.base;

import atlantafx.base.controls.RingProgressIndicator;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.prefs.AppPrefs;
import javafx.beans.binding.Bindings;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

public class AppMainWindowContentComp extends SimpleComp {

    @Override
    protected Region createSimple() {
        var overlay = AppDialog.getModalOverlay();
        var loaded = AppMainWindow.getInstance().getLoadedContent();
        var bg = Comp.of(() -> {
            var loading = new RingProgressIndicator(-1, false);
            loading.visibleProperty().bind(Bindings.not(AppPrefs.get().performanceMode()).and(overlay.isNull()));
            loading.setPrefWidth(60);
            loading.setPrefHeight(60);

            var pane = new StackPane(loading);
            pane.getStyleClass().add("background");

            loaded.subscribe(region -> {
                if (region != null) {
                    pane.getChildren().setAll(region);
                }
            });

            return pane;
        });
        var modal = new ModalOverlayComp(bg, overlay);
        return modal.createRegion();
    }
}
