package io.xpipe.app.comp.store;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.ScrollComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategoryConfig;
import io.xpipe.app.storage.DataStoreColor;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.store.DataStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.LinkedHashMap;

@AllArgsConstructor
public class StoreCategoryConfigComp extends SimpleComp {

    public static void show(StoreCategoryWrapper wrapper) {
        var config = new SimpleObjectProperty<>(wrapper.getCategory().getConfig());
        var comp = new ScrollComp(new StoreCategoryConfigComp(wrapper, config));
        comp.prefWidth(500);
        var modal = ModalOverlay.of(AppI18n.observable("categoryConfigTitle", wrapper.getName().getValue()), comp, null);
        modal.addButton(ModalButton.cancel());
        modal.addButton(ModalButton.ok(() -> {
            DataStorage.get().updateCategoryConfig(wrapper.getCategory(), config.getValue());
        }));
        modal.show();
    }

    private final StoreCategoryWrapper wrapper;
    private final Property<DataStoreCategoryConfig> config;

    @Override
    protected Region createSimple() {
        var colors = new LinkedHashMap<ObservableValue<String>, OptionsBuilder>();
        colors.put(AppI18n.observable("none"), new OptionsBuilder());
        for (DataStoreColor value : DataStoreColor.values()) {
            colors.put(AppI18n.observable(value.getId()), new OptionsBuilder());
        }

        var c = config.getValue();
        var color = new SimpleIntegerProperty(c.getColor() != null ? Arrays.asList(DataStoreColor.values()).indexOf(c.getColor()) : 0);
        var scripts = new SimpleObjectProperty<>(c.getDontAllowScripts());
        var confirm = new SimpleObjectProperty<>(c.getConfirmAllModifications());
        var sync = new SimpleObjectProperty<>(c.getSync());
        var ref = new SimpleObjectProperty<>(c.getDefaultIdentityStore() != null ? DataStorage.get().getStoreEntryIfPresent(c.getDefaultIdentityStore()).map(
                DataStoreEntry::ref).orElse(null) : null);
        var options = new OptionsBuilder()
                .nameAndDescription("categorySync")
                .addToggle(sync)
                .hide(!DataStorage.get().supportsSharing() || !wrapper.getCategory().canShare())
                .nameAndDescription("categoryDontAllowScripts")
                .addToggle(scripts)
                .nameAndDescription("categoryConfirmAllModifications")
                .addToggle(confirm)
                .nameAndDescription("categoryDefaultIdentity")
                .addComp(StoreChoiceComp.other(ref, DataStore.class, s -> true, StoreViewState.get().getAllIdentitiesCategory()), ref)
                .nameAndDescription("categoryColor")
                .choice(color, colors)
                .bind(() -> {
                    return new DataStoreCategoryConfig(color.get() > 0 ? DataStoreColor.values()[color.get() - 1] : null, scripts.get(), confirm.get(), sync.get(), ref.get() != null ? ref.get().get().getUuid() : null);
                }, config)
                .buildComp();
        return options.createRegion();
    }
}
