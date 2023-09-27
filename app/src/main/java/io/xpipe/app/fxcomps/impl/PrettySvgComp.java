package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.impl.FileNames;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;

public class PrettySvgComp extends SimpleComp {

    private final ObservableValue<String> value;
    private final double width;
    private final double height;

    public PrettySvgComp(ObservableValue<String> value, double width, double height) {
        this.value = value;
        this.width = width;
        this.height = height;
    }

    @Override
    protected Region createSimple() {
        var image = new SimpleStringProperty();
        var syncValue = PlatformThread.sync(value);
        var storeIcon = SvgView.create(Bindings.createObjectBinding(
                () -> {
                    if (image.get() == null) {
                        return null;
                    }

                    if (AppImages.hasSvgImage(image.getValue())) {
                        return AppImages.svgImage(image.getValue());
                    } else if (AppImages.hasSvgImage(image.getValue().replace("-dark", ""))) {
                        return AppImages.svgImage(image.getValue().replace("-dark", ""));
                    } else {
                        return null;
                    }
                },
                image));
        var ar = Bindings.createDoubleBinding(
                () -> {
                    return storeIcon.getWidth().getValue().doubleValue()
                            / storeIcon.getHeight().getValue().doubleValue();
                },
                storeIcon.getWidth(),
                storeIcon.getHeight());
        var widthProperty = Bindings.createDoubleBinding(
                () -> {
                    boolean widthLimited = width / height < ar.doubleValue();
                    if (widthLimited) {
                        return width;
                    } else {
                        return height * ar.doubleValue();
                    }
                },
                ar);
        var heightProperty = Bindings.createDoubleBinding(
                () -> {
                    boolean widthLimited = width / height < ar.doubleValue();
                    if (widthLimited) {
                        return width / ar.doubleValue();
                    } else {
                        return height;
                    }
                },
                ar);

        var stack = new StackPane();
        var node = storeIcon.createWebview();
        node.prefWidthProperty().bind(widthProperty);
        node.maxWidthProperty().bind(widthProperty);
        node.minWidthProperty().bind(widthProperty);
        node.prefHeightProperty().bind(heightProperty);
        node.maxHeightProperty().bind(heightProperty);
        node.minHeightProperty().bind(heightProperty);
        stack.getChildren().add(node);

        Consumer<String> update = val -> {
            var fixed = val != null ? FileNames.getBaseName(val) + (AppPrefs.get().theme.get().getTheme().isDarkMode() ? "-dark" : "") + "." + FileNames.getExtension(val) : null;
            image.set(fixed);
        };

        SimpleChangeListener.apply(syncValue, val -> update.accept(val));
        AppPrefs.get().theme.addListener((observable, oldValue, newValue) -> {
            update.accept(syncValue.getValue());
        });

        stack.setFocusTraversable(false);
        stack.setPrefWidth(width);
        stack.setMinWidth(width);
        stack.setPrefHeight(height);
        stack.setMinHeight(height);
        stack.setAlignment(Pos.CENTER);
        stack.getStyleClass().add("stack");
        return stack;
    }
}
