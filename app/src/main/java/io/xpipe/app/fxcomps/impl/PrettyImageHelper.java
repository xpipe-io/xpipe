package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.core.AppImages;
import io.xpipe.app.fxcomps.Comp;
import io.xpipe.core.store.FileNames;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

public class PrettyImageHelper {

    public static Comp<?> ofFixedSquare(String img, int size) {
        if (img != null && img.endsWith(".svg")) {
            var base = FileNames.getBaseName(img);
            var renderedName = base + "-" + size + ".png";
            if (AppImages.hasNormalImage(base + "-" + size + ".png")) {
                return new PrettyImageComp(new SimpleStringProperty(renderedName), size, size);
            } else {
                return new PrettySvgComp(new SimpleStringProperty(img), size, size);
            }
        }

        return new PrettyImageComp(new SimpleStringProperty(img), size, size);
    }

    public static Comp<?> ofFixed(String img, int w, int h) {
        if (w == h) {
            return ofFixedSquare(img, w);
        }

        return img.endsWith(".svg") ? new PrettySvgComp(new SimpleStringProperty(img), w, h) : new PrettyImageComp(new SimpleStringProperty(img), w,
                h);
    }


    public static Comp<?> ofSvg(ObservableValue<String> img, int w, int h) {
        return new PrettySvgComp(img, w, h);
    }

    public static Comp<?> ofFixedSmallSquare(String img) {
        return ofFixed(img, 16, 16);
    }

}
