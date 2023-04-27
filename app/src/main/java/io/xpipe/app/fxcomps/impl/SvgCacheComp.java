package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import javafx.animation.PauseTransition;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.util.Duration;

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
        svgFile.addListener((observable, oldValue, newValue) -> {
            var pt = new PauseTransition();
            pt.setDuration(Duration.millis(1000));
            pt.setOnFinished(actionEvent -> {
                if (newValue == null || cache.getCached(newValue).isPresent()) {
                    return;
                }

                if (!newValue.equals(svgFile.getValue())) {
                    return;
                }

                WritableImage image = back.snapshot(null, null);
                if (image.getWidth() < 10) {
                    return;
                }

                cache.put(newValue, image);
            });
            pt.play();
        });
        back.prefWidthProperty().bind(width);
        back.prefHeightProperty().bind(height);

        svgFile.addListener((observable, oldValue, newValue) -> {
            var cached = cache.getCached(newValue);
            webViewContent.setValue(newValue != null || cached.isEmpty() ? AppImages.svgImage(newValue) : null);
            frontContent.setValue(cached.orElse(null));
            back.setVisible(cached.isEmpty());
            front.setVisible(cached.isPresent());
        });

        var stack = new StackPane(back, front);
        stack.prefWidthProperty().bind(width);
        stack.prefHeightProperty().bind(height);
        return stack;
    }
}
