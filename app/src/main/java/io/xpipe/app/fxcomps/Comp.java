package io.xpipe.app.fxcomps;

import atlantafx.base.controls.Spacer;
import io.xpipe.app.fxcomps.augment.Augment;
import io.xpipe.app.fxcomps.augment.GrowAugment;
import io.xpipe.app.fxcomps.util.Shortcuts;
import io.xpipe.app.fxcomps.util.SimpleChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Comp<S extends CompStructure<?>> {

    private List<Augment<S>> augments;

    public static Comp<CompStructure<Region>> empty() {
        return of(() -> new Region());
    }

    public static Comp<CompStructure<Spacer>> spacer(double size) {
        return of(() -> new Spacer(size));
    }

    public static <R extends Region> Comp<CompStructure<R>> of(Supplier<R> r) {
        return new Comp<>() {
            @Override
            public CompStructure<R> createBase() {
                return new SimpleCompStructure<>(r.get());
            }
        };
    }

    public static Comp<CompStructure<Separator>> separator() {
        return of(() -> new Separator(Orientation.HORIZONTAL));
    }

    @SuppressWarnings("unchecked")
    public static <IR extends Region, SIN extends CompStructure<IR>, OR extends Region> Comp<CompStructure<OR>> derive(
            Comp<SIN> comp, Function<IR, OR> r) {
        return of(() -> r.apply((IR) comp.createRegion()));
    }

    @SuppressWarnings("unchecked")
    public <T extends Comp<S>> T apply(Augment<S> augment) {
        if (augments == null) {
            augments = new ArrayList<>();
        }
        augments.add(augment);
        return (T) this;
    }

    public Comp<S> prefWidth(int width) {
        return apply(struc -> struc.get().setPrefWidth(width));
    }

    public Comp<S> prefHeight(int height) {
        return apply(struc -> struc.get().setPrefHeight(height));
    }

    public Comp<S> hgrow() {
        return apply(struc -> HBox.setHgrow(struc.get(), Priority.ALWAYS));
    }

    public Comp<S> vgrow() {
        return apply(struc -> VBox.setVgrow(struc.get(), Priority.ALWAYS));
    }

    public Comp<S> focusTraversable() {
        return apply(struc -> struc.get().setFocusTraversable(true));
    }

    public Comp<S> focusTraversable(boolean b) {
        return apply(struc -> struc.get().setFocusTraversable(b));
    }

    public Comp<S> visible(ObservableValue<Boolean> o) {
        return apply(struc -> struc.get().visibleProperty().bind(o));
    }

    public Comp<S> disable(ObservableValue<Boolean> o) {
        return apply(struc -> struc.get().disableProperty().bind(o));
    }

    public Comp<S> padding(Insets insets) {
        return apply(struc -> struc.get().setPadding(insets));
    }

    public Comp<S> hide(ObservableValue<Boolean> o) {
        return apply(struc -> {
            var region = struc.get();
            SimpleChangeListener.apply(o, n -> {
                if (!n) {
                    region.setVisible(true);
                    region.setManaged(true);
                } else {
                    region.setVisible(false);
                    region.setManaged(false);
                }
            });
        });
    }

    public Comp<S> styleClass(String styleClass) {
        return apply(struc -> struc.get().getStyleClass().add(styleClass));
    }

    public Comp<S> accessibleText(String text) {
        return apply(struc -> struc.get().setAccessibleText(text));
    }

    public Comp<S> grow(boolean width, boolean height) {
        return apply(GrowAugment.create(width, height));
    }

    public Comp<S> shortcut(KeyCombination shortcut, Consumer<S> con) {
        return apply(struc -> Shortcuts.addShortcut(struc.get(), shortcut, r -> con.accept(struc)));
    }

    public Comp<S> shortcut(KeyCombination shortcut) {
        return apply(struc -> Shortcuts.addShortcut((ButtonBase) struc.get(), shortcut));
    }

    public Comp<S> tooltip(Supplier<String> text) {
        return apply(r -> Tooltip.install(r.get(), new Tooltip(text.get())));
    }

    public Region createRegion() {
        return createStructure().get();
    }

    public S createStructure() {
        S struc = createBase();
        if (augments != null) {
            for (var a : augments) {
                a.augment(struc);
            }
        }
        return struc;
    }

    public abstract S createBase();
}
