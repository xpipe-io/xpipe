package io.xpipe.app.comp.base;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.core.AppDisplayScale;
import io.xpipe.app.core.AppImages;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.core.FilePath;

import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.Optional;
import java.util.stream.IntStream;

public class PrettyImageHelper {

    private static Optional<String> rasterizedImageIfExists(String img, int height) {
        if (img != null && img.endsWith(".svg")) {
            var base = FilePath.of(img).getBaseName();
            var renderedName = base + "-" + height + ".png";
            if (AppImages.hasImage(renderedName)) {
                return Optional.of(renderedName);
            }
        }

        if (img != null && img.endsWith(".png")) {
            if (AppImages.hasImage(img)) {
                return Optional.of(img);
            }
        }

        return Optional.empty();
    }

    private static String rasterizedImageIfExistsScaled(String img, int height, int... availableSizes) {
        if (img == null) {
            return null;
        }

        if (!img.endsWith(".svg")) {
            return rasterizedImageIfExists(img, height).orElse(null);
        }

        var scale = AppDisplayScale.getEffectiveDisplayScale();
        var mult = Math.round(scale * height);
        var base = FilePath.of(img).getBaseName();
        var available = IntStream.of(availableSizes)
                .filter(integer -> AppImages.hasImage(base + "-" + integer + ".png"))
                .boxed()
                .toList();
        var closest = available.stream()
                .filter(integer -> integer >= mult)
                .findFirst()
                .orElse(available.size() > 0 ? available.getLast() : 0);
        return rasterizedImageIfExists(img, closest).orElse(null);
    }

    public static BaseRegionBuilder<?, ?> ofFixedSizeSquare(String img, int size) {
        return ofFixedSize(img, size, size);
    }

    public static BaseRegionBuilder<?, ?> ofFixedSize(String img, int w, int h) {
        return ofFixedSize(new SimpleStringProperty(img), w, h);
    }

    public static BaseRegionBuilder<?, ?> ofFixedSize(ObservableValue<String> img, int w, int h) {
        if (img == null) {
            return new PrettyImageComp(new SimpleStringProperty(null), w, h);
        }

        var binding = BindingsHelper.map(img, s -> {
            return rasterizedImageIfExistsScaled(s, h, 16, 24, 40, 80);
        });
        return new PrettyImageComp(binding, w, h);
    }

    public static BaseRegionBuilder<?, ?> ofSpecificFixedSize(String img, int w, int h) {
        var b = rasterizedImageIfExistsScaled(img, h, h, h * 2);
        return new PrettyImageComp(new ReadOnlyStringWrapper(b), w, h);
    }
}
