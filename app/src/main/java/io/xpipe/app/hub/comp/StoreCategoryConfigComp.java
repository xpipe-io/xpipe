package io.xpipe.app.hub.comp;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.SimpleRegionBuilder;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.comp.base.ToggleGroupComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategoryConfig;
import io.xpipe.app.storage.DataStoreEntry;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.Region;

import lombok.AllArgsConstructor;

import java.util.LinkedHashMap;

@AllArgsConstructor
public class StoreCategoryConfigComp extends SimpleRegionBuilder {

    private final StoreCategoryWrapper wrapper;
    private final Property<DataStoreCategoryConfig> config;

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

    private RegionBuilder<?> createToggle(Property<Boolean> prop, Boolean inherited) {
        var map = new LinkedHashMap<Boolean, ObservableValue<String>>();
        map.put(Boolean.FALSE, AppI18n.observable("no"));
        map.put(
                null,
                AppI18n.observable("inherit", inherited != null && inherited ? AppI18n.get("yes") : AppI18n.get("no")));
        map.put(Boolean.TRUE, AppI18n.observable("yes"));
        var comp = new ToggleGroupComp<>(prop, new SimpleObjectProperty<>(map));
        return comp;
    }

    @Override
    protected Region createSimple() {
        var parents = DataStorage.get().getCategoryParentHierarchy(wrapper.getCategory());
        var parentConfig = parents.size() > 1
                ? DataStorage.get().getEffectiveCategoryConfig(parents.get(parents.size() - 2))
                : DataStoreCategoryConfig.empty();

        var c = config.getValue();
        var scripts = new SimpleObjectProperty<>(c.getDontAllowScripts());
        var confirm = new SimpleObjectProperty<>(c.getConfirmAllModifications());
        var sync = new SimpleObjectProperty<>(c.getSync());
        var freeze = new SimpleObjectProperty<>(c.getFreezeConfigurations());
        var ref = new SimpleObjectProperty<>(
                c.getDefaultIdentityStore() != null
                        ? DataStorage.get()
                                .getStoreEntryIfPresent(c.getDefaultIdentityStore())
                                .map(DataStoreEntry::ref)
                                .orElse(null)
                        : null);
        var connectionsCategory = wrapper.getRoot().equals(StoreViewState.get().getAllConnectionsCategory());

        var options = new OptionsBuilder();

        var specialCategorySync = !wrapper.getCategory().canShare();
        var syncDisable = !DataStorage.get().supportsSync()
                || ((sync.getValue() == null || !sync.getValue())
                        && !wrapper.getCategory().canShare());
        options.name(
                        specialCategorySync
                                ? AppI18n.observable(
                                        "categorySyncSpecial", wrapper.getName().getValue())
                                : AppI18n.observable("categorySync"))
                .description("categorySyncDescription")
                .addComp(createToggle(sync, parentConfig.getSync()), sync)
                .disable(syncDisable)
                .nameAndDescription("categoryDontAllowScripts")
                .addComp(createToggle(scripts, parentConfig.getDontAllowScripts()), scripts)
                .hide(!connectionsCategory)
                .nameAndDescription("categoryConfirmAllModifications")
                .addComp(createToggle(confirm, parentConfig.getConfirmAllModifications()), confirm)
                .hide(!connectionsCategory)
                .nameAndDescription("categoryFreeze")
                .addComp(createToggle(freeze, parentConfig.getFreezeConfigurations()), freeze)
                .hide(!connectionsCategory)
                .nameAndDescription("categoryDefaultIdentity")
                .addComp(
                        new StoreChoiceComp<>(
                                null,
                                ref,
                                DataStore.class,
                                null,
                                StoreViewState.get().getAllIdentitiesCategory()),
                        ref)
                .hide(!connectionsCategory)
                .bind(
                        () -> {
                            return new DataStoreCategoryConfig(
                                    c.getColor(),
                                    scripts.get(),
                                    confirm.get(),
                                    sync.get(),
                                    freeze.get(),
                                    ref.get() != null ? ref.get().get().getUuid() : null);
                        },
                        config);
        var r = options.build();
        var sp = new ScrollPane(r);
        sp.setFitToWidth(true);
        sp.prefHeightProperty().bind(r.heightProperty());
        return sp;
    }
}
