package io.xpipe.app.comp.base;

import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.store.DataStore;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;
import lombok.Setter;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

@AllArgsConstructor
public class StoreToggleComp extends SimpleComp {

    private final String nameKey;
    private final StoreSection section;
    private final BooleanProperty value;
    private final Consumer<Boolean> onChange;

    @Setter
    private ObservableBooleanValue customVisibility = new SimpleBooleanProperty(true);

    public static <T extends DataStore> StoreToggleComp simpleToggle(String nameKey, StoreSection section, Function<T, Boolean> initial, BiConsumer<T, Boolean> setter) {
        return new StoreToggleComp(nameKey, section,new SimpleBooleanProperty(initial.apply(section.getWrapper().getEntry().getStore().asNeeded())),v -> {
            setter.accept(section.getWrapper().getEntry().getStore().asNeeded(),v);
        });
    }

    public static <T extends DataStore> StoreToggleComp childrenToggle(String nameKey, StoreSection section, Function<T, Boolean> initial, BiConsumer<T, Boolean> setter) {
        return new StoreToggleComp(nameKey, section,new SimpleBooleanProperty(initial.apply(section.getWrapper().getEntry().getStore().asNeeded())),v -> {
            setter.accept(section.getWrapper().getEntry().getStore().asNeeded(),v);
            section.getWrapper().refreshChildren();
        });
    }

    public StoreToggleComp(String nameKey, StoreSection section, boolean initial, Consumer<Boolean> onChange) {
        this.nameKey = nameKey;
        this.section = section;
        this.value = new SimpleBooleanProperty(initial);
        this.onChange = onChange;
    }

    public StoreToggleComp(String nameKey, StoreSection section, BooleanProperty initial, Consumer<Boolean> onChange) {
        this.nameKey = nameKey;
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
        var t = new ToggleSwitchComp(value, AppI18n.observable(nameKey))
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
