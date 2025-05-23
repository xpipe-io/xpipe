package io.xpipe.app.comp.store;

import io.xpipe.app.comp.SimpleComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
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
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

import lombok.AllArgsConstructor;

import java.util.Arrays;
import java.util.LinkedHashMap;

@AllArgsConstructor
public class StoreCategoryConfigComp extends SimpleComp {

    public static void show(StoreCategoryWrapper wrapper) {
        var config = new SimpleObjectProperty<>(wrapper.getCategory().getConfig());
        var comp = new StoreCategoryConfigComp(wrapper, config);
        comp.prefWidth(600);
        var modal = ModalOverlay.of(
                AppI18n.observable("categoryConfigTitle", wrapper.getName().getValue()), comp, null);
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
        var color = new SimpleIntegerProperty(
                c.getColor() != null ? Arrays.asList(DataStoreColor.values()).indexOf(c.getColor()) + 1 : 0);
        var scripts = new SimpleObjectProperty<>(c.getDontAllowScripts());
        var confirm = new SimpleObjectProperty<>(c.getConfirmAllModifications());
        var sync = new SimpleObjectProperty<>(c.getSync());
        var readOnly = new SimpleObjectProperty<>(c.getReadOnly());
        var ref = new SimpleObjectProperty<>(
                c.getDefaultIdentityStore() != null
                        ? DataStorage.get()
                                .getStoreEntryIfPresent(c.getDefaultIdentityStore())
                                .map(DataStoreEntry::ref)
                                .orElse(null)
                        : null);
        var connectionsCategory = wrapper.getRoot().equals(StoreViewState.get().getAllConnectionsCategory());
        var options = new OptionsBuilder()
                .nameAndDescription("categorySync")
                .addYesNoToggle(sync)
                .hide(!DataStorage.get().supportsSync()
                        || !wrapper.getCategory().canShare())
                .nameAndDescription("categoryDontAllowScripts")
                .addYesNoToggle(scripts)
                .hide(!connectionsCategory)
                //                .nameAndDescription("categoryConfirmAllModifications")
                //                .addYesNoToggle(confirm)
                //                .hide(!connectionsCategory)
                .nameAndDescription("categoryReadOnly")
                .addYesNoToggle(readOnly)
                .nameAndDescription("categoryDefaultIdentity")
                .addComp(
                        StoreChoiceComp.other(
                                ref,
                                DataStore.class,
                                s -> true,
                                StoreViewState.get().getAllIdentitiesCategory()),
                        ref)
                .hide(!connectionsCategory)
                .nameAndDescription("categoryColor")
                .choice(color, colors)
                .bind(
                        () -> {
                            return new DataStoreCategoryConfig(
                                    color.get() > 0 ? DataStoreColor.values()[color.get() - 1] : null,
                                    scripts.get(),
                                    confirm.get(),
                                    sync.get(),
                                    readOnly.get(),
                                    ref.get() != null ? ref.get().get().getUuid() : null);
                        },
                        config)
                .buildComp();
        var r = options.createRegion();
        var sp = new ScrollPane(r);
        sp.setFitToWidth(true);
        sp.prefHeightProperty().bind(r.heightProperty());
        return sp;
    }
}
