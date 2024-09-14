package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.resources.AppImages;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.core.store.FileNames;

import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.Optional;

public class PrettyImageHelper {

    private static Optional<String> rasterizedImageIfExists(String img, int width, int height) {
        if (img != null && img.endsWith(".svg")) {
            var base = FileNames.getBaseName(img);
            var renderedName = base + "-" + height + ".png";
            if (AppImages.hasNormalImage(base + "-" + height + ".png")) {
                return Optional.of(renderedName);
            }
        }

        return Optional.empty();
    }

    public static Comp<?> ofFixedSizeSquare(String img, int size) {
        return ofFixedSize(img, size, size);
    }

    public static Comp<?> ofFixedRasterized(String img, int w, int h) {
        if (img == null) {
            return new PrettyImageComp(new SimpleStringProperty(null), w, h);
        }

        var rasterized = rasterizedImageIfExists(img, w, h);
        return new PrettyImageComp(new SimpleStringProperty(rasterized.orElse(null)), w, h);
    }

    public static Comp<?> ofFixedSize(String img, int w, int h) {
        if (img == null) {
            return new PrettyImageComp(new SimpleStringProperty(null), w, h);
        }

        var rasterized = rasterizedImageIfExists(img, w, h);
        if (rasterized.isPresent()) {
            return new PrettyImageComp(new SimpleStringProperty(rasterized.get()), w, h);
        } else {
            return img.endsWith(".svg")
                    ? new PrettySvgComp(new SimpleStringProperty(img), w, h)
                    : new PrettyImageComp(new SimpleStringProperty(img), w, h);
        }
    }

    public static Comp<?> ofFixedSize(ObservableValue<String> img, int w, int h) {
        if (img == null) {
            return new PrettyImageComp(new SimpleStringProperty(null), w, h);
        }

        var binding = BindingsHelper.map(img, s -> {
            return rasterizedImageIfExists(s, w, h).orElse(s);
        });
        return new PrettyImageComp(binding, w, h);
    }
}
