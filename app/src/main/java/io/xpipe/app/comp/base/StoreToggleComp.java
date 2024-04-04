package io.xpipe.app.comp.base;

import io.xpipe.app.comp.store.StoreSection;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.ThreadHelper;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

import java.util.function.Consumer;

@AllArgsConstructor
public class StoreToggleComp extends SimpleComp {

    private final String nameKey;
    private final StoreSection section;
    private final BooleanProperty value;
    private final Consumer<Boolean> onChange;

    public StoreToggleComp(String nameKey, StoreSection section, boolean initial, Consumer<Boolean> onChange) {
        this.nameKey = nameKey;
        this.section = section;
        this.value = new SimpleBooleanProperty(initial);
        this.onChange = onChange;
    }

    @Override
    protected Region createSimple() {
        var disable = section.getWrapper().getValidity().map(state -> state != DataStoreEntry.Validity.COMPLETE);
        var visible = Bindings.createBooleanBinding(
                () -> {
                    return section.getWrapper().getValidity().getValue() == DataStoreEntry.Validity.COMPLETE
                            && section.getShowDetails().get();
                },
                section.getWrapper().getValidity(),
                section.getShowDetails());
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
