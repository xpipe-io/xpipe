package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.core.AppImages;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.core.FilePath;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableDoubleValue;
import javafx.beans.value.ObservableValue;

import java.util.Optional;
import java.util.stream.IntStream;

public class PrettyImageHelper {

    private static Optional<String> rasterizedImageIfExists(String img, int height) {
        if (img != null && img.endsWith(".svg")) {
            var base = FilePath.of(img).getBaseName();
            var renderedName = base + "-" + height + ".png";
            if (AppImages.hasNormalImage(renderedName)) {
                return Optional.of(renderedName);
            }
        }

        if (img != null && img.endsWith(".png")) {
            if (AppImages.hasNormalImage(img)) {
                return Optional.of(img);
            }
        }

        return Optional.empty();
    }

    private static ObservableValue<String> rasterizedImageIfExistsScaled(
            String img, int height, int... availableSizes) {
        ObservableDoubleValue obs =
                AppMainWindow.get() != null ? AppMainWindow.get().displayScale() : new SimpleDoubleProperty(1.0);
        return Bindings.createStringBinding(
                () -> {
                    if (img == null) {
                        return null;
                    }

                    if (!img.endsWith(".svg")) {
                        return rasterizedImageIfExists(img, height).orElse(null);
                    }

                    var mult = Math.round(obs.get() * height);
                    var base = FilePath.of(img).getBaseName();
                    var available = IntStream.of(availableSizes)
                            .filter(integer -> AppImages.hasNormalImage(base + "-" + integer + ".png"))
                            .boxed()
                            .toList();
                    var closest = available.stream()
                            .filter(integer -> integer >= mult)
                            .findFirst()
                            .orElse(available.size() > 0 ? available.getLast() : 0);
                    return rasterizedImageIfExists(img, closest).orElse(null);
                },
                obs);
    }

    public static Comp<?> ofFixedSizeSquare(String img, int size) {
        return ofFixedSize(img, size, size);
    }

    public static Comp<?> ofFixedSize(String img, int w, int h) {
        return ofFixedSize(new SimpleStringProperty(img), w, h);
    }

    public static Comp<?> ofFixedSize(ObservableValue<String> img, int w, int h) {
        if (img == null) {
            return new PrettyImageComp(new SimpleStringProperty(null), w, h);
        }

        var binding = BindingsHelper.flatMap(img, s -> {
            return rasterizedImageIfExistsScaled(s, h, 16, 24, 40, 80);
        });
        return new PrettyImageComp(binding, w, h);
    }

    public static Comp<?> ofSpecificFixedSize(String img, int w, int h) {
        var b = rasterizedImageIfExistsScaled(img, h, h, h * 2);
        return new PrettyImageComp(b, w, h);
    }
}
