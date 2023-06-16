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

        var active = new SimpleObjectProperty<AnimationTimer>();
        var webViewContent = new SimpleStringProperty();
        var back = SvgView.create(webViewContent).createWebview();
        back.prefWidthProperty().bind(width);
        back.prefHeightProperty().bind(height);
        svgFile.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                back.setVisible(false);
                front.setVisible(false);
                return;
            }

            var cached = cache.getCached(newValue);
            if (cached.isPresent()) {
                frontContent.setValue(cached.get());
                back.setVisible(false);
                front.setVisible(true);
                return;
            }

            webViewContent.setValue(AppImages.svgImage(newValue));
            back.setVisible(true);
            front.setVisible(false);

            AnimationTimer timer = new AnimationTimer() {
                int frames = 0;
                final AnimationTimer instance = this;

                @Override
                public void handle(long l) {
                    if (++frames == 30) {
                        stop();
                        SnapshotParameters parameters = new SnapshotParameters();
                        parameters.setFill(Color.TRANSPARENT);
                        if (!instance.equals(active.get())) {
                            active.set(null);
                            return;
                        }
                        active.set(null);

                        WritableImage image = back.snapshot(parameters, null);
                        if (image.getWidth() < 10) {
                            return;
                        }

                        if (cache.getCached(newValue).isPresent()) {
                            return;
                        }

                        if (!newValue.equals(svgFile.getValue())) {
                            return;
                        }

                        var found = false;
                        out:
                        for (int x = 0; x < image.getWidth(); x++) {
                            for (int y = 0; y < image.getHeight(); y++) {
                                if (image.getPixelReader().getArgb(x, y) != 0x00000000) {
                                    found = true;
                                    break out;
                                }
                            }
                        }
                        if (!found) {
                            return;
                        }

                        System.out.println("cache " + newValue);
                        cache.put(newValue, image);
                    }
                }
            };
            Platform.runLater(() -> {
                //                timer.start();
                //                active.set(timer);
            });
        });

        var stack = new StackPane(back, front);
        stack.prefWidthProperty().bind(width);
        stack.prefHeightProperty().bind(height);
        return stack;
    }
}
