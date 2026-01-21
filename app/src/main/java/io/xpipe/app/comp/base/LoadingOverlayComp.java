package io.xpipe.app.comp.base;



import io.xpipe.app.comp.RegionBuilder;

import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.StackPane;
import org.int4.fx.builders.common.AbstractRegionBuilder;
import io.xpipe.app.comp.BaseRegionBuilder;

public class LoadingOverlayComp extends RegionBuilder<StackPane> {

    private final BaseRegionBuilder<?,?> comp;
    private final ObservableValue<Boolean> loading;
    private final boolean showIcon;

    public LoadingOverlayComp(BaseRegionBuilder<?,?> comp, ObservableValue<Boolean> loading, boolean showIcon) {
        this.comp = comp;
        this.loading = loading;
        this.showIcon = showIcon;
    }

    @Override
    public StackPane createSimple() {
        var r = comp.build();
        var loadingOverlay = new StackPane();
        if (showIcon) {
            var loading = new LoadingIconComp(this.loading, AppFontSizes::title).build();
            loading.prefWidthProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                return Math.min(r.getHeight() - 20, 50);
                            },
                            r.heightProperty()));
            loading.prefHeightProperty().bind(loading.prefWidthProperty());
            loading.managedProperty().bind(loading.visibleProperty());
            loadingOverlay.getChildren().add(loading);
        }
        loadingOverlay.getStyleClass().add("loading-comp");
        loadingOverlay.setVisible(showIcon && this.loading.getValue());
        loadingOverlay.setManaged(showIcon && this.loading.getValue());

        var listener = new ChangeListener<Boolean>() {
            @Override
            public void changed(ObservableValue<? extends Boolean> observable, Boolean oldValue, Boolean busy) {
                if (!busy) {
                    // Reduce flickering for consecutive loads
                    ThreadHelper.runAsync(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }

                        if (!LoadingOverlayComp.this.loading.getValue()) {
                            Platform.runLater(() -> {
                                loadingOverlay.setVisible(false);
                                loadingOverlay.setManaged(false);
                            });
                        }
                    });
                } else {
                    ThreadHelper.runAsync(() -> {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException ignored) {
                        }

                        if (LoadingOverlayComp.this.loading.getValue()) {
                            Platform.runLater(() -> {
                                loadingOverlay.setVisible(true);
                                loadingOverlay.setManaged(true);
                            });
                        }
                    });
                }
            }
        };
        this.loading.addListener(listener);

        var stack = new StackPane(r, loadingOverlay);

        stack.prefWidthProperty().bind(r.prefWidthProperty());
        stack.prefHeightProperty().bind(r.prefHeightProperty());

        return stack;
    }
}
