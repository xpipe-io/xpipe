package io.xpipe.app.comp;

import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.PlatformThread;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import org.int4.fx.builders.common.AbstractRegionBuilder;
import io.xpipe.app.comp.BaseRegionBuilder;

import java.util.function.Consumer;

public abstract class BaseRegionBuilder<T extends Region, B extends BaseRegionBuilder<T, B>> extends AbstractRegionBuilder<T, B> {

    public BaseRegionBuilder() {
        apply(t -> {
            BindingsHelper.preserve(t, BaseRegionBuilder.this);
        });
    }

    public B hgrow() {
        apply(t -> HBox.setHgrow(t, Priority.ALWAYS));
        return self();
    }

    public B vgrow() {
        apply(t -> VBox.setVgrow(t, Priority.ALWAYS));
        return self();
    }

    public B describe(Consumer<RegionDescriptor.RegionDescriptorBuilder> c) {
        apply(r -> {
            var b = RegionDescriptor.builder();
            c.accept(b);
            b.build().apply(r);
        });
        return self();
    }

    public B visible(ObservableValue<Boolean> o) {
        return apply(struc -> {
            var region = struc;
            BindingsHelper.preserve(region, o);
            o.subscribe(n -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    region.setVisible(n);
                });
            });
        });
    }

    public B padding(Insets insets) {
        return apply(struc -> struc.setPadding(insets));
    }

    public B disable(ObservableValue<Boolean> o) {
        return apply(struc -> {
            var region = struc;
            BindingsHelper.preserve(region, o);
            o.subscribe(n -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    region.setDisable(n);
                });
            });
        });
    }

    public B show(ObservableValue<Boolean> when) {
        return this.hide(when.map((b) -> !b).orElse(true));
    }

    public B hide(ObservableValue<Boolean> o) {
        return apply(struc -> {
            var region = struc;
            BindingsHelper.preserve(region, o);
            o.subscribe(n -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    if (!n) {
                        region.setVisible(true);
                        region.setManaged(true);
                    } else {
                        region.setVisible(false);
                        region.setManaged(false);
                    }
                });
            });
        });
    }
}
