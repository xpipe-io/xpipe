package io.xpipe.extension.fxcomps.impl;

import io.xpipe.extension.fxcomps.SimpleComp;
import io.xpipe.extension.fxcomps.util.PlatformThread;
import io.xpipe.extension.util.XPipeDaemon;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;

public class PrettyImageComp extends SimpleComp {

    private final ObservableValue<String> value;
    private final double width;
    private final double height;

    public PrettyImageComp(ObservableValue<String> value, double width, double height) {
        this.value = value;
        this.width = width;
        this.height = height;
    }

    @Override
    protected Region createSimple() {
        var aspectRatioProperty = new SimpleDoubleProperty(1);
        var widthProperty = Bindings.createDoubleBinding(
                () -> {
                    boolean widthLimited = width / height < aspectRatioProperty.doubleValue();
                    if (widthLimited) {
                        return width;
                    } else {
                        return height * aspectRatioProperty.doubleValue();
                    }
                },
                aspectRatioProperty);
        var heightProperty = Bindings.createDoubleBinding(
                () -> {
                    boolean widthLimited = width / height < aspectRatioProperty.doubleValue();
                    if (widthLimited) {
                        return width / aspectRatioProperty.doubleValue();
                    } else {
                        return height;
                    }
                },
                aspectRatioProperty);

        Node node;

        if (value.getValue().endsWith(".svg")) {
            var storeIcon = SvgComp.create(
                    Bindings.createStringBinding(() -> XPipeDaemon.getInstance().svgImage(value.getValue()), value));
            aspectRatioProperty.bind(Bindings.createDoubleBinding(
                    () -> {
                        return storeIcon.getWidth().getValue().doubleValue()
                                / storeIcon.getHeight().getValue().doubleValue();
                    },
                    storeIcon.getWidth(),
                    storeIcon.getHeight()));
            node = storeIcon.createWebview();
            ((WebView) node).prefWidthProperty().bind(widthProperty);
            ((WebView) node).maxWidthProperty().bind(widthProperty);
            ((WebView) node).minWidthProperty().bind(widthProperty);
            ((WebView) node).prefHeightProperty().bind(heightProperty);
            ((WebView) node).maxHeightProperty().bind(heightProperty);
            ((WebView) node).minHeightProperty().bind(heightProperty);
        } else {
            var storeIcon = new ImageView();
            storeIcon
                    .imageProperty()
                    .bind(Bindings.createObjectBinding(
                            () -> {
                                var image = XPipeDaemon.getInstance().image(value.getValue());
                                aspectRatioProperty.set(image.getWidth() / image.getHeight());
                                return image;
                            },
                            PlatformThread.sync(value)));
            storeIcon.fitWidthProperty().bind(widthProperty);
            storeIcon.fitHeightProperty().bind(heightProperty);
            storeIcon.setSmooth(true);
            node = storeIcon;
        }

        var stack = new StackPane(node);
        stack.setPrefWidth(width);
        stack.setMinWidth(width);
        stack.setPrefHeight(height);
        stack.setMinHeight(height);
        stack.setAlignment(Pos.CENTER);
        return stack;
    }
}
