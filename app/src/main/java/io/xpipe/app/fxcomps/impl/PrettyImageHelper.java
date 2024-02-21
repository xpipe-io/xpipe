package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.core.store.FileNames;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.Optional;

public class PrettyImageHelper {

    public static Optional<Comp<?>> rasterizedIfExists(String img, int width, int height) {
        if (img != null && img.endsWith(".svg")) {
            var base = FileNames.getBaseName(img);
            var renderedName = base + "-" + height + ".png";
            if (AppImages.hasNormalImage(base + "-" + height + ".png")) {
                return Optional.of(new PrettyImageComp(new SimpleStringProperty(renderedName), width, height));
            }
        }

        return Optional.empty();
    }

    public static Comp<?> ofFixedSquare(String img, int size) {
        return ofFixedSize(img, size, size);
    }

    public static Comp<?> ofFixedSize(String img, int w, int h) {
        if (img == null) {
            return new PrettyImageComp(new SimpleStringProperty(null), w, h);
        }

        var rasterized = rasterizedIfExists(img, w, h);
        if (rasterized.isPresent()) {
            return rasterized.get();
        } else {
            return img.endsWith(".svg")
                    ? new PrettySvgComp(new SimpleStringProperty(img), w, h)
                    : new PrettyImageComp(new SimpleStringProperty(img), w, h);
        }
    }

    public static Comp<?> ofSvg(ObservableValue<String> img, int w, int h) {
        return new PrettySvgComp(img, w, h);
    }

    public static Comp<?> ofRasterized(ObservableValue<String> img, int w, int h) {
        return new PrettyImageComp(img, w, h);
    }

    public static Comp<?> ofFixedSmallSquare(String img) {
        return ofFixedSize(img, 16, 16);
    }
}
