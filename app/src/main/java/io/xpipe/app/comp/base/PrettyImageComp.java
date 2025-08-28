package io.xpipe.app.comp.base;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.core.AppImages;
import io.xpipe.app.issue.TrackEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.core.FilePath;

import javafx.beans.binding.Bindings;
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
        var storeIcon = new ImageView();
        var aspectRatioProperty = Bindings.createDoubleBinding(
                () -> {
                    if (storeIcon.getImage() == null) {
                        return 1.0;
                    }

                    return storeIcon.getImage().getWidth()
                            / storeIcon.getImage().getHeight();
                },
                storeIcon.imageProperty());
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

        storeIcon.setFocusTraversable(false);
        storeIcon
                .imageProperty()
                .bind(Bindings.createObjectBinding(
                        () -> {
                            if (image.get() == null) {
                                return null;
                            }

                            var value = image.getValue();
                            if (AppImages.hasNormalImage(value)) {
                                return AppImages.image(value);
                            } else if (AppImages.hasNormalImage(value.replace("-dark", ""))) {
                                return AppImages.image(value.replace("-dark", ""));
                            } else {
                                TrackEvent.withWarn("Image file not found")
                                        .tag("file", value)
                                        .handle();
                                return null;
                            }
                        },
                        image));
        storeIcon.fitWidthProperty().bind(widthProperty);
        storeIcon.fitHeightProperty().bind(heightProperty);
        storeIcon.setSmooth(true);
        stack.getChildren().add(storeIcon);

        Consumer<String> update = val -> {
            PlatformThread.runLaterIfNeeded(() -> {
                var useDark = AppPrefs.get() != null
                        && AppPrefs.get().theme().getValue() != null
                        && AppPrefs.get().theme().getValue().isDark();
                var fixed = val != null
                        ? FilePath.of(val).getBaseName() + (useDark ? "-dark" : "") + "."
                                + FilePath.of(val).getExtension().orElseThrow()
                        : null;
                image.set(fixed);

                if (val == null) {
                    stack.getChildren().getFirst().setVisible(false);
                } else {
                    stack.getChildren().getFirst().setVisible(true);
                }
            });
        };

        value.subscribe(update);
        if (AppPrefs.get() != null) {
            AppPrefs.get().theme().addListener((observable, oldValue, newValue) -> {
                update.accept(value.getValue());
            });
        }

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
