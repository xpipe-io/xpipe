package io.xpipe.app.comp;

import io.xpipe.app.comp.augment.Augment;
import io.xpipe.app.comp.augment.GrowAugment;
import io.xpipe.app.comp.base.TooltipAugment;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.PlatformThread;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.Spacer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Comp<S extends CompStructure<?>> {

    private List<Augment<S>> augments;

    public static Comp<CompStructure<Region>> empty() {
        return of(() -> new Region());
    }

    public static Comp<CompStructure<Spacer>> hspacer(double size) {
        return of(() -> new Spacer(size));
    }

    public static Comp<CompStructure<Spacer>> hspacer() {
        return of(() -> new Spacer(Orientation.HORIZONTAL));
    }

    public static Comp<CompStructure<Spacer>> vspacer() {
        return of(() -> new Spacer(Orientation.VERTICAL));
    }

    public static Comp<CompStructure<Spacer>> vspacer(double size) {
        return of(() -> new Spacer(size, Orientation.VERTICAL));
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

    public <T extends Comp<S>> T onSceneAssign(Augment<S> augment) {
        return apply(struc -> struc.get().sceneProperty().addListener((observable, oldValue, newValue) -> {
            if (newValue != null) {
                augment.augment(struc);
            }
        }));
    }

    public void focusOnShow() {
        onSceneAssign(struc -> {
            Platform.runLater(() -> {
                Platform.runLater(() -> {
                    struc.get().requestFocus();
                });
            });
        });
    }

    public Comp<S> minWidth(double width) {
        return apply(struc -> struc.get().setMinWidth(width));
    }

    public Comp<S> minHeight(double height) {
        return apply(struc -> struc.get().setMinHeight(height));
    }

    public Comp<S> maxWidth(int width) {
        return apply(struc -> struc.get().setMaxWidth(width));
    }

    public Comp<S> maxHeight(int height) {
        return apply(struc -> struc.get().setMaxHeight(height));
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

    public Comp<S> focusTraversableForAccessibility() {
        return apply(struc -> struc.get().focusTraversableProperty().bind(Platform.accessibilityActiveProperty()));
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

    public Comp<S> styleClass(String styleClass) {
        return apply(struc -> struc.get().getStyleClass().add(styleClass));
    }

    public Comp<S> accessibleText(ObservableValue<String> text) {
        return apply(struc -> struc.get().accessibleTextProperty().bind(text));
    }

    public Comp<S> accessibleText(String text) {
        return apply(struc -> struc.get().setAccessibleText(text));
    }

    public Comp<S> accessibleTextKey(String key) {
        return apply(struc -> struc.get().accessibleTextProperty().bind(AppI18n.observable(key)));
    }

    public Comp<S> grow(boolean width, boolean height) {
        return apply(GrowAugment.create(width, height));
    }

    public Comp<S> tooltip(ObservableValue<String> text) {
        return apply(new TooltipAugment<>(text, null));
    }

    public Comp<S> tooltipKey(String key) {
        return apply(new TooltipAugment<>(key, null));
    }

    public Comp<S> tooltipKey(String key, KeyCombination shortcut) {
        return apply(new TooltipAugment<>(key, shortcut));
    }

    public Region createRegion() {
        return createStructure().get();
    }

    public S createStructure() {
        S struc = createBase();
        // Make comp last at least as long as region
        BindingsHelper.preserve(struc.get(), this);
        if (augments != null) {
            for (var a : augments) {
                a.augment(struc);
            }
        }
        return struc;
    }

    public abstract S createBase();
}
