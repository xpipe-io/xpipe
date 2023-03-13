package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.comp.storage.store.StoreEntryFlatMiniSection;
import io.xpipe.app.comp.storage.store.StoreEntryWrapper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.CustomComboBoxBuilder;
import io.xpipe.app.util.XPipeDaemon;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;

@AllArgsConstructor
public class ShellStoreChoiceComp<T extends ShellStore> extends SimpleComp {

    public static ShellStoreChoiceComp<ShellStore> proxy(Property<ShellStore> selected) {
        return new ShellStoreChoiceComp<>(Mode.PROXY_CHOICE, null, selected, ShellStore.class, shellStore -> true);
    }

    public static ShellStoreChoiceComp<ShellStore> host(Property<ShellStore> selected) {
        return new ShellStoreChoiceComp<>(Mode.HOST_CHOICE, null, selected, ShellStore.class, shellStore -> true);
    }

    public static ShellStoreChoiceComp<ShellStore> proxy(ShellStore self, Property<ShellStore> selected) {
        return new ShellStoreChoiceComp<>(Mode.PROXY_CHOICE, self, selected, ShellStore.class, shellStore -> true);
    }

    public static ShellStoreChoiceComp<ShellStore> host(ShellStore self, Property<ShellStore> selected) {
        return new ShellStoreChoiceComp<>(Mode.HOST_CHOICE, self, selected, ShellStore.class, shellStore -> true);
    }

    public static enum Mode {
        HOST_CHOICE,
        PROXY_CHOICE
    }

    private final Mode mode;
    private final T self;
    private final Property<T> selected;
    private final Class<T> storeClass;
    private final Predicate<T> applicableCheck;

    protected Region createGraphic(T s) {
        var provider = DataStoreProviders.byStore(s);
        var imgView =
                new PrettyImageComp(new SimpleStringProperty(provider.getDisplayIconFileName()), 16, 16).createRegion();

        var name = DataStorage.get().getUsableStores().stream()
                .filter(e -> e.equals(s))
                .findAny()
                .flatMap(store -> {
                    if (ShellStore.isLocal(store.asNeeded()) && mode == Mode.PROXY_CHOICE) {
                        return Optional.of(AppI18n.get("none"));
                    }

                    return XPipeDaemon.getInstance().getStoreName(store);
                })
                .orElse(AppI18n.get("unknown"));

        return new Label(name, imgView);
    }

    @Override
    @SuppressWarnings("unchecked")
    protected Region createSimple() {
        var map = StoreEntryFlatMiniSection.createMap();
        var comboBox = new CustomComboBoxBuilder<T>(
                selected,
                t -> map.entrySet().stream()
                        .filter(e -> t.equals(e.getKey().getEntry().getStore()))
                        .findFirst()
                        .orElseThrow()
                        .getValue(),
                new Label(AppI18n.get("none")),
                n -> true);
        comboBox.setSelectedDisplay(t -> createGraphic(t));
        comboBox.setUnknownNode(t -> createGraphic(t));

        for (Map.Entry<StoreEntryWrapper, Region> e : map.entrySet()) {
            if (e.getKey().getEntry().getStore() == self) {
                continue;
            }

            var s = e.getKey().getEntry().getStore();
            if (!storeClass.isAssignableFrom(s.getClass()) || !applicableCheck.test((T) s)) {
                continue;
            }

            comboBox.add((T) e.getKey().getEntry().getStore());
        }

        ComboBox<Node> cb = comboBox.build();
        cb.getStyleClass().add("choice-comp");
        cb.setMaxWidth(2000);
        return cb;
    }
}
