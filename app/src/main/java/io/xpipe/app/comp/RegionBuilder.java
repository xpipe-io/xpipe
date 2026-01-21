package io.xpipe.app.comp;

import atlantafx.base.controls.Spacer;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.layout.Region;
import org.int4.fx.builders.custom.CustomRegionBuilder;

import java.util.ArrayList;
import java.util.function.Supplier;

public abstract class RegionBuilder<T extends Region> extends BaseRegionBuilder<T, RegionBuilder<T>> {

    public static RegionBuilder<Region> empty() {
        return of(() -> {
            var r = new Region();
            r.getStyleClass().add("empty");
            return r;
        });
    }

    public static RegionBuilder<Spacer> hspacer() {
        return of(() -> new Spacer(Orientation.HORIZONTAL));
    }

    public static RegionBuilder<Spacer> hspacer(double size) {
        return of(() -> new Spacer(size));
    }

    public static RegionBuilder<Spacer> vspacer() {
        return of(() -> new Spacer(Orientation.VERTICAL));
    }

    public static RegionBuilder<Spacer> vspacer(double size) {
        return of(() -> new Spacer(size, Orientation.VERTICAL));
    }

    public static RegionBuilder<Separator> hseparator() {
        return of(() -> new Separator(Orientation.HORIZONTAL));
    }

    public static RegionBuilder<Separator> vseparator() {
        return of(() -> new Separator(Orientation.VERTICAL));
    }

    public static <R extends Region> RegionBuilder<R> of(Supplier<R> r) {
        return new RegionBuilder<R>() {

            @Override
            protected R createSimple() {
                return r.get();
            }
        };
    }

    @Override
    public final T build() {
        var r = createSimple();
        initialize(r);
        return r;
    }

    protected abstract T createSimple();
}
