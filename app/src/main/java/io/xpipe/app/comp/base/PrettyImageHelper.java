package io.xpipe.app.comp.base;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.core.App;
import io.xpipe.app.resources.AppImages;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.core.store.FileNames;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableValue;

import java.util.List;
import java.util.Optional;

public class PrettyImageHelper {

    private static Optional<String> rasterizedImageIfExists(String img, int height) {
        if (img != null && img.endsWith(".svg")) {
            var base = FileNames.getBaseName(img);
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

    private static ObservableValue<String> rasterizedImageIfExistsScaled(String img, int height) {
        return Bindings.createStringBinding(
                () -> {
                    if (img == null) {
                        return null;
                    }

                    if (!img.endsWith(".svg")) {
                        return rasterizedImageIfExists(img, height).orElse(null);
                    }

                    var sizes = List.of(16, 24, 40, 80);
                    var mult = Math.round(App.getApp().displayScale().get() * height);
                    var base = FileNames.getBaseName(img);
                    var available = sizes.stream()
                            .filter(integer -> AppImages.hasNormalImage(base + "-" + integer + ".png"))
                            .toList();
                    var closest = available.stream()
                            .filter(integer -> integer >= mult)
                            .findFirst()
                            .orElse(available.size() > 0 ? available.getLast() : 0);
                    return rasterizedImageIfExists(img, closest).orElse(null);
                },
                App.getApp().displayScale());
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
            return rasterizedImageIfExistsScaled(s, h);
        });
        return new PrettyImageComp(binding, w, h);
    }
}
