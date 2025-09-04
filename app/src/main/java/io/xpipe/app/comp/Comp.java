package io.xpipe.app.comp;

import io.xpipe.app.comp.augment.Augment;
import io.xpipe.app.comp.base.TooltipHelper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.PlatformThread;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.ReadOnlyBooleanWrapper;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Insets;
import javafx.geometry.Orientation;
import javafx.scene.Node;
import javafx.scene.control.Separator;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import atlantafx.base.controls.Spacer;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

public abstract class Comp<S extends CompStructure<?>> {

    private List<Augment<S>> augments;

    public static Comp<CompStructure<Region>> empty() {
        return of(() -> {
            var r = new Region();
            r.getStyleClass().add("empty");
            return r;
        });
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

    public static Comp<CompStructure<Separator>> hseparator() {
        return of(() -> new Separator(Orientation.HORIZONTAL));
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
                    Platform.runLater(() -> {
                        struc.get().requestFocus();
                    });
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
        return apply(struc -> {
            var region = struc.get();
            BindingsHelper.preserve(region, o);
            o.subscribe(n -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    region.setVisible(n);
                });
            });
        });
    }

    public Comp<S> disable(ObservableValue<Boolean> o) {
        return apply(struc -> {
            var region = struc.get();
            BindingsHelper.preserve(region, o);
            o.subscribe(n -> {
                PlatformThread.runLaterIfNeeded(() -> {
                    region.setDisable(n);
                });
            });
        });
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
        return apply(struc -> {
            struc.get().parentProperty().addListener((c, o, n) -> {
                if (o instanceof Region) {
                    if (width) {
                        struc.get().prefWidthProperty().unbind();
                    }
                    if (height) {
                        struc.get().prefHeightProperty().unbind();
                    }
                }

                bindGrow(struc.get(), n, width, height);
            });

            bindGrow(struc.get(), struc.get().getParent(), width, height);
        });
    }

    private void bindGrow(Region r, Node parent, boolean width, boolean height) {
        if (!(parent instanceof Region p)) {
            return;
        }

        if (width) {
            r.prefWidthProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                var val = p.getWidth()
                                        - p.getInsets().getLeft()
                                        - p.getInsets().getRight();
                                if (val <= 0) {
                                    return Region.USE_COMPUTED_SIZE;
                                }

                                // Floor to prevent rounding issues which cause an infinite growing
                                return Math.floor(val);
                            },
                            p.widthProperty(),
                            p.insetsProperty()));
        }
        if (height) {
            r.prefHeightProperty()
                    .bind(Bindings.createDoubleBinding(
                            () -> {
                                var val = p.getHeight()
                                        - p.getInsets().getTop()
                                        - p.getInsets().getBottom();
                                if (val <= 0) {
                                    return Region.USE_COMPUTED_SIZE;
                                }

                                // Floor to prevent rounding issues which cause an infinite growing
                                return Math.floor(val);
                            },
                            p.heightProperty(),
                            p.insetsProperty()));
        }
    }

    public Comp<S> tooltip(ObservableValue<String> text) {
        return apply(struc -> {
            var tt = TooltipHelper.create(text, null);
            Tooltip.install(struc.get(), tt);
        });
    }

    public Comp<S> tooltipKey(String key) {
        return apply(struc -> {
            var tt = TooltipHelper.create(AppI18n.observable(key), null);
            Tooltip.install(struc.get(), tt);
        });
    }

    public Comp<S> tooltipKey(String key, KeyCombination shortcut) {
        return apply(struc -> {
            var tt = TooltipHelper.create(AppI18n.observable(key), shortcut);
            Tooltip.install(struc.get(), tt);
        });
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
