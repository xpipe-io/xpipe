package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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

        var image = new SimpleStringProperty();
        var currentNode = new SimpleObjectProperty<Node>();
        SimpleChangeListener.apply(PlatformThread.sync(value), val -> {
            image.set(val);
            var requiresChange = val == null || (val.endsWith(".svg") && !(currentNode.get() instanceof WebView) ||
                    !(currentNode.get() instanceof ImageView));
            if (!requiresChange) {
                return;
            }

            aspectRatioProperty.unbind();

            if (val == null) {
                currentNode.set(new Region());
            }

            else if (val.endsWith(".svg")) {
                var storeIcon = SvgView.create(
                        Bindings.createStringBinding(() -> {
                            if (!AppImages.hasSvgImage(image.getValue())) {
                                return null;
                            }

                            return AppImages.svgImage(image.getValue());
                        }, image));
                var ar = Bindings.createDoubleBinding(
                        () -> {
                            return storeIcon.getWidth().getValue().doubleValue()
                                    / storeIcon.getHeight().getValue().doubleValue();
                        },
                        storeIcon.getWidth(),
                        storeIcon.getHeight());
                aspectRatioProperty.bind(ar);
                var node = storeIcon.createWebview();
                ((WebView) node).prefWidthProperty().bind(widthProperty);
                ((WebView) node).maxWidthProperty().bind(widthProperty);
                ((WebView) node).minWidthProperty().bind(widthProperty);
                ((WebView) node).prefHeightProperty().bind(heightProperty);
                ((WebView) node).maxHeightProperty().bind(heightProperty);
                ((WebView) node).minHeightProperty().bind(heightProperty);
                currentNode.set(node);
            } else {
                var storeIcon = new ImageView();
                storeIcon.setFocusTraversable(false);
                storeIcon
                        .imageProperty()
                        .bind(Bindings.createObjectBinding(
                                () -> {
                                    if (!AppImages.hasNormalImage(image.getValue())) {
                                        return null;
                                    }

                                    return AppImages.image(image.getValue());
                                },
                                image));
                var ar = Bindings.createDoubleBinding(
                        () -> {
                            if (storeIcon.getImage() == null) {
                                return 1.0;
                            }

                            return storeIcon.getImage().getWidth() / storeIcon.getImage().getHeight();
                        },
                        storeIcon.imageProperty());
                aspectRatioProperty.bind(ar);
                storeIcon.fitWidthProperty().bind(widthProperty);
                storeIcon.fitHeightProperty().bind(heightProperty);
                storeIcon.setSmooth(true);
                currentNode.set(storeIcon);
            }
        });

        var stack = new StackPane();
        SimpleChangeListener.apply(currentNode, val -> {
            if (val == null) {
                stack.getChildren().clear();
                return;
            }

            stack.getChildren().setAll(val);
        });
        stack.setFocusTraversable(false);
        stack.setPrefWidth(width);
        stack.setMinWidth(width);
        stack.setPrefHeight(height);
        stack.setMinHeight(height);
        stack.setAlignment(Pos.CENTER);
        return stack;
    }
}
