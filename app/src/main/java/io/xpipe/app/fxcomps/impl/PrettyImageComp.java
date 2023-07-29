package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.PlatformThread;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.impl.FileNames;
import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;

import java.util.function.Consumer;

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
        var imageAspectRatioProperty = new SimpleDoubleProperty(1);
        var svgAspectRatioProperty = new SimpleDoubleProperty(1);
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
        var stack = new StackPane();

        {
            var svgImageContent = new SimpleStringProperty();
            var storeIcon = SvgView.create(svgImageContent);
            SimpleChangeListener.apply(image, newValue -> {
                if (newValue == null) {
                    svgImageContent.set(null);
                    return;
                }

                if (AppImages.hasSvgImage(newValue)) {
                    svgImageContent.set(AppImages.svgImage(newValue));
                } else if (AppImages.hasSvgImage(newValue.replace("-dark", ""))) {
                    svgImageContent.set(AppImages.svgImage(newValue.replace("-dark", "")));
                } else {
                    svgImageContent.set(null);
                }
            });
            var ar = Bindings.createDoubleBinding(
                    () -> {
                        return storeIcon.getWidth().getValue().doubleValue()
                                / storeIcon.getHeight().getValue().doubleValue();
                    },
                    storeIcon.getWidth(),
                    storeIcon.getHeight());
            svgAspectRatioProperty.bind(ar);
            var node = storeIcon.createWebview();
            node.prefWidthProperty().bind(widthProperty);
            node.maxWidthProperty().bind(widthProperty);
            node.minWidthProperty().bind(widthProperty);
            node.prefHeightProperty().bind(heightProperty);
            node.maxHeightProperty().bind(heightProperty);
            node.minHeightProperty().bind(heightProperty);
            stack.getChildren().add(node);
        }

        {
            var storeIcon = new ImageView();
            storeIcon.setFocusTraversable(false);
            storeIcon
                    .imageProperty()
                    .bind(Bindings.createObjectBinding(
                            () -> {
                                if (image.get() == null) {
                                    return null;
                                }

                                if (AppImages.hasNormalImage(image.getValue())) {
                                    return AppImages.image(image.getValue());
                                } else if (AppImages.hasNormalImage(image.getValue().replace("-dark", ""))) {
                                    return AppImages.image(image.getValue().replace("-dark", ""));
                                } else {
                                    return null;
                                }
                            },
                            image));
            var ar = Bindings.createDoubleBinding(
                    () -> {
                        if (storeIcon.getImage() == null) {
                            return 1.0;
                        }

                        return storeIcon.getImage().getWidth()
                                / storeIcon.getImage().getHeight();
                    },
                    storeIcon.imageProperty());
            imageAspectRatioProperty.bind(ar);
            storeIcon.fitWidthProperty().bind(widthProperty);
            storeIcon.fitHeightProperty().bind(heightProperty);
            storeIcon.setSmooth(true);
            stack.getChildren().add(storeIcon);
        }

        Consumer<String> update = val -> {
            var fixed = val != null ? FileNames.getBaseName(val) + (AppPrefs.get().theme.get().getTheme().isDarkMode() ? "-dark" : "") + "." + FileNames.getExtension(val) : null;
            image.set(fixed);
            aspectRatioProperty.unbind();

            if (val == null) {
                stack.getChildren().get(0).setOpacity(0.0);
                stack.getChildren().get(1).setOpacity(0.0);
            } else if (val.endsWith(".svg")) {
                aspectRatioProperty.bind(svgAspectRatioProperty);
                stack.getChildren().get(0).setOpacity(1.0);
                stack.getChildren().get(1).setOpacity(0.0);
            } else {
                aspectRatioProperty.bind(imageAspectRatioProperty);
                stack.getChildren().get(0).setOpacity(0.0);
                stack.getChildren().get(1).setOpacity(1.0);
            }
        };

        SimpleChangeListener.apply(PlatformThread.sync(value), val -> update.accept(val));
        AppPrefs.get().theme.addListener((observable, oldValue, newValue) -> {
            update.accept(value.getValue());
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
