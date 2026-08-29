package io.xpipe.app.hub.entry;

import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ToggleSwitchComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.list.StoreViewState;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.util.ThreadHelper;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;

import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor
public class StoreToggleComp extends SimpleRegionBuilder {

    private final String nameKey;
    private final ObservableValue<LabelGraphic> graphic;
    private final StoreSection section;
    private final BooleanProperty value;
    private final Consumer<Boolean> onChange;

    @Setter
    private ObservableValue<Boolean> customVisibility = new SimpleBooleanProperty(true);

    public StoreToggleComp(
            String nameKey,
            ObservableValue<LabelGraphic> graphic,
            StoreSection section,
            BooleanProperty value,
            Consumer<Boolean> onChange) {
        this.nameKey = nameKey;
        this.graphic = graphic;
        this.section = section;
        this.value = value;
        this.onChange = onChange;
    }

    public static <T extends DataStore> StoreToggleComp enableToggle(
            String nameKey, StoreSection section, BooleanProperty value, BiConsumer<T, Boolean> setter) {
        var val = new SimpleBooleanProperty();
        ObservableValue<LabelGraphic> g = val.map(aBoolean -> aBoolean
                ? new LabelGraphic.IconGraphic("mdi2c-circle-slice-8")
                : new LabelGraphic.IconGraphic("mdi2p-power"));
        var t = new StoreToggleComp(nameKey, g, section, value, v -> {
            setter.accept(section.getEntry().getStore().asNeeded(), v);
        });
        t.value.subscribe((newValue) -> {
            val.set(newValue);
        });
        return t;
    }

    public static <T extends DataStore> StoreToggleComp childrenToggle(
            boolean graphic, StoreSection section, Function<T, Boolean> initial, BiConsumer<T, Boolean> setter) {
        return childrenToggle("showNonRunningChildren", graphic, section, initial, setter);
    }

    public static <T extends DataStore> StoreToggleComp childrenToggle(
            String nameKey,
            boolean graphic,
            StoreSection section,
            Function<T, Boolean> mapper,
            BiConsumer<T, Boolean> setter) {
        var val = new SimpleBooleanProperty(
                mapper.apply(section.getEntry().getStore().asNeeded()));
        ObservableValue<LabelGraphic> g = graphic
                ? val.map(aBoolean -> aBoolean
                        ? new LabelGraphic.IconGraphic("mdi2e-eye-plus")
                        : new LabelGraphic.IconGraphic("mdi2e-eye-minus"))
                : null;
        var t = new StoreToggleComp(null, g, section, val, v -> {
            Platform.runLater(() -> {
                setter.accept(section.getEntry().getStore().asNeeded(), v);
                StoreViewState.get().triggerStoreListVisibilityUpdate();
            });
        });
        t.describe(d -> d.nameKey(nameKey));

        t.value.subscribe((newValue) -> {
            val.set(newValue);
        });

        section.getWrapper().getPersistentState().addListener((observable, oldValue, newValue) -> {
            val.set(mapper.apply(section.getEntry().getStore().asNeeded()));
        });

        return t;
    }

    @Override
    protected Region createSimple() {
        var disable = section.getWrapper().getValidity().map(state -> state != DataStoreEntry.Validity.COMPLETE);
        var visible = Bindings.createBooleanBinding(
                () -> {
                    if (!this.customVisibility.getValue()) {
                        return false;
                    }

                    return section.getWrapper().getValidity().getValue() == DataStoreEntry.Validity.COMPLETE;
                },
                section.getWrapper().getValidity(),
                this.customVisibility);
        var t = new ToggleSwitchComp(value, AppI18n.observable(nameKey), graphic)
                .show(visible)
                .disable(disable);
        t.describe(d -> d.nameKey("toggleEnabled"));
        value.addListener((observable, oldValue, newValue) -> {
            ThreadHelper.runAsync(() -> {
                onChange.accept(newValue);
            });
        });
        return t.build();
    }
}
