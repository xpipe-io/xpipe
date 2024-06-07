package io.xpipe.app.comp.base;

import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.comp.store.StoreViewState;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.LabelGraphic;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;
import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor
public class StoreToggleComp extends SimpleComp {

    private final String nameKey;
    private final ObservableValue<LabelGraphic> graphic;
    private final StoreSection section;
    private final BooleanProperty value;
    private final Consumer<Boolean> onChange;

    @Setter
    private ObservableBooleanValue customVisibility = new SimpleBooleanProperty(true);

    public static <T extends DataStore> StoreToggleComp simpleToggle(
            String nameKey, ObservableValue<LabelGraphic> graphic, StoreSection section, Function<T, Boolean> initial, BiConsumer<T, Boolean> setter) {
        return new StoreToggleComp(
                nameKey,
                graphic,
                section,
                new SimpleBooleanProperty(
                        initial.apply(section.getWrapper().getEntry().getStore().asNeeded())),
                v -> {
                    setter.accept(section.getWrapper().getEntry().getStore().asNeeded(), v);
                });
    }

    public static <T extends DataStore> StoreToggleComp childrenToggle(
            String nameKey, boolean graphic, StoreSection section, Function<T, Boolean> initial, BiConsumer<T, Boolean> setter) {
        var val = new SimpleBooleanProperty();
        ObservableValue<LabelGraphic> g = graphic ? val.map(aBoolean -> aBoolean ?
                new LabelGraphic.IconGraphic("mdi2c-circle-slice-8") : new LabelGraphic.IconGraphic("mdi2c-circle-half-full")) : null;
        var t = new StoreToggleComp(nameKey, g, section,
                new SimpleBooleanProperty(initial.apply(section.getWrapper().getEntry().getStore().asNeeded())), v -> {
            Platform.runLater(() -> {
                setter.accept(section.getWrapper().getEntry().getStore().asNeeded(), v);
                StoreViewState.get().toggleStoreListUpdate();
            });
        });
        t.value.subscribe((newValue) -> {
            val.set(newValue);
        });
        return t;
    }

    public StoreToggleComp(String nameKey, ObservableValue<LabelGraphic> graphic, StoreSection section, boolean initial, Consumer<Boolean> onChange) {
        this.nameKey = nameKey;
        this.graphic = graphic;
        this.section = section;
        this.value = new SimpleBooleanProperty(initial);
        this.onChange = onChange;
    }

    public StoreToggleComp(String nameKey, ObservableValue<LabelGraphic> graphic, StoreSection section, BooleanProperty initial, Consumer<Boolean> onChange) {
        this.nameKey = nameKey;
        this.graphic = graphic;
        this.section = section;
        this.value = initial;
        this.onChange = onChange;
    }

    @Override
    protected Region createSimple() {
        var disable = section.getWrapper().getValidity().map(state -> state != DataStoreEntry.Validity.COMPLETE);
        var visible = Bindings.createBooleanBinding(
                () -> {
                    if (!this.customVisibility.get()) {
                        return false;
                    }

                    return section.getWrapper().getValidity().getValue() == DataStoreEntry.Validity.COMPLETE;
                },
                section.getWrapper().getValidity(),
                section.getShowDetails(),
                this.customVisibility);
        var t = new ToggleSwitchComp(value, AppI18n.observable(nameKey), graphic)
                .visible(visible)
                .disable(disable);
        value.addListener((observable, oldValue, newValue) -> {
            ThreadHelper.runAsync(() -> {
                onChange.accept(newValue);
            });
        });
        return t.createRegion();
    }
}
