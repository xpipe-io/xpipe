package io.xpipe.app.comp.base;

import io.xpipe.app.comp.storage.store.StoreSection;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.fxcomps.util.BindingsHelper;
import io.xpipe.app.storage.DataStoreEntry;
import javafx.beans.binding.Bindings;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.scene.layout.Region;

import java.util.function.Consumer;

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
        var disable = section.getWrapper().getState().map(state -> state != DataStoreEntry.State.COMPLETE_AND_VALID);
        var visible = BindingsHelper.persist(Bindings.createBooleanBinding(
                () -> {
                    return section.getWrapper().getState().getValue() == DataStoreEntry.State.COMPLETE_AND_VALID
                            && section.getShowDetails().get();
                },
                section.getWrapper().getState(),
                section.getShowDetails()));
        var t = new NamedToggleComp(value, AppI18n.observable(nameKey))
                .visible(visible)
                .disable(disable);
        value.addListener((observable, oldValue, newValue) -> onChange.accept(newValue));
        return t.createRegion();
    }
}
