package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.animation.AnimationTimer;
import javafx.application.Platform;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.SnapshotParameters;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;

public class SvgCacheComp extends SimpleComp {

    private final ObservableValue<Number> width;
    private final ObservableValue<Number> height;
    private final ObservableValue<String> svgFile;
    private final SvgCache cache;

    public SvgCacheComp(
            ObservableValue<Number> width,
            ObservableValue<Number> height,
            ObservableValue<String> svgFile,
            SvgCache cache) {
        this.width = PlatformThread.sync(width);
        this.height = PlatformThread.sync(height);
        this.svgFile = PlatformThread.sync(svgFile);
        this.cache = cache;
    }

    @Override
    protected Region createSimple() {
        var frontContent = new SimpleObjectProperty<Image>();
        var front = new ImageView();
        front.fitWidthProperty().bind(width);
        front.fitHeightProperty().bind(height);
        front.setSmooth(true);
        frontContent.addListener((observable, oldValue, newValue) -> {
            front.setImage(newValue);
        });

        var webViewContent = new SimpleStringProperty();
        var back = SvgView.create(webViewContent).createWebview();
        back.prefWidthProperty().bind(width);
        back.prefHeightProperty().bind(height);
        svgFile.addListener((observable, oldValue, newValue) -> {
            var cached = cache.getCached(newValue);
            webViewContent.setValue(newValue != null || cached.isEmpty() ? AppImages.svgImage(newValue) : null);
            frontContent.setValue(cached.orElse(null));
            back.setVisible(cached.isEmpty());
            front.setVisible(cached.isPresent());

            if (cached.isPresent()) {
                return;
            }

            Platform.runLater(() -> new AnimationTimer() {
                int frames = 0;

                @Override
                public void handle(long l) {
                    if (++frames == 2) {
                        SnapshotParameters parameters = new SnapshotParameters();
                        parameters.setFill(Color.TRANSPARENT);
                        back.snapshot(snapshotResult -> {
                            WritableImage image = snapshotResult.getImage();
                            if (image.getWidth() < 10) {
                                return null;
                            }

                            if (cache.getCached(newValue).isPresent()) {
                                return null;
                            }

                            cache.put(newValue, image);
                            return null;
                        }, parameters, null);
                        stop();
                    }
                }
            }.start());
        });

        var stack = new StackPane(back, front);
        stack.prefWidthProperty().bind(width);
        stack.prefHeightProperty().bind(height);
        return stack;
    }
}
