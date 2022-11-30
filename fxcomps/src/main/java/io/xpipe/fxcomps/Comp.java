package io.xpipe.fxcomps;

import io.xpipe.fxcomps.augment.Augment;
import io.xpipe.fxcomps.augment.GrowAugment;
import io.xpipe.fxcomps.comp.WrapperComp;
import io.xpipe.fxcomps.util.Shortcuts;
import io.xpipe.fxcomps.util.SimpleChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ButtonBase;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCombination;
import javafx.scene.layout.Region;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

public abstract class Comp<S extends CompStructure<?>> {

    private List<Augment<S>> augments;

    public static <R extends Region> Comp<CompStructure<R>> of(Supplier<R> r) {
        return new WrapperComp<>(() -> {
            var region = r.get();
            return () -> region;
        });
    }

    public static <S extends CompStructure<?>> Comp<S> ofStructure(Supplier<S> r) {
        return new WrapperComp<>(r);
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

    public Comp<S> disable(ObservableValue<Boolean> o) {
        return apply(struc -> struc.get().disableProperty().bind(o));
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

    public Comp<S> grow(boolean width, boolean height) {
        return apply(GrowAugment.create(false, false));
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
