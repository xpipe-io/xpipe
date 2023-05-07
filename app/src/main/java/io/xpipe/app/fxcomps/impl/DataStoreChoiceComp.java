package io.xpipe.app.fxcomps.impl;

import io.xpipe.app.comp.storage.store.StoreEntryFlatMiniSectionComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreProviders;
import io.xpipe.app.fxcomps.SimpleComp;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.CustomComboBoxBuilder;
import io.xpipe.app.util.XPipeDaemon;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.LeafShellStore;
import io.xpipe.core.store.ShellStore;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.layout.Region;
import lombok.AllArgsConstructor;

import java.util.Optional;
import java.util.function.Predicate;

@AllArgsConstructor
public class DataStoreChoiceComp<T extends DataStore> extends SimpleComp {

    public static DataStoreChoiceComp<ShellStore> proxy(Property<ShellStore> selected) {
        return new DataStoreChoiceComp<>(Mode.PROXY, null, selected, ShellStore.class, shellStore -> true);
    }

    public static DataStoreChoiceComp<ShellStore> host(Property<ShellStore> selected) {
        return new DataStoreChoiceComp<>(Mode.HOST, null, selected, ShellStore.class, shellStore -> true);
    }

    public static DataStoreChoiceComp<ShellStore> environment(ShellStore self, Property<ShellStore> selected) {
        return new DataStoreChoiceComp<>(Mode.ENVIRONMENT, self, selected, ShellStore.class, shellStore -> true);
    }

    public static DataStoreChoiceComp<ShellStore> proxy(ShellStore self, Property<ShellStore> selected) {
        return new DataStoreChoiceComp<>(Mode.PROXY, self, selected, ShellStore.class, shellStore -> true);
    }

    public static DataStoreChoiceComp<ShellStore> host(ShellStore self, Property<ShellStore> selected) {
        return new DataStoreChoiceComp<>(Mode.HOST, self, selected, ShellStore.class, shellStore -> true);
    }

    public static enum Mode {
        HOST,
        ENVIRONMENT,
        OTHER,
        PROXY
    }

    private final Mode mode;
    private final T self;
    private final Property<T> selected;
    private final Class<T> storeClass;
    private final Predicate<T> applicableCheck;

    protected Region createGraphic(T s) {
        var provider = DataStoreProviders.byStore(s);
        var imgView =
                new PrettyImageComp(new SimpleStringProperty(provider.getDisplayIconFileName(s)), 16, 16).createRegion();

        var name = DataStorage.get().getUsableStores().stream()
                .filter(e -> e.equals(s))
                .findAny()
                .flatMap(store -> {
                    if (ShellStore.isLocal(store.asNeeded()) && mode == Mode.PROXY) {
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
        var list = StoreEntryFlatMiniSectionComp.ALL;
        var comboBox = new CustomComboBoxBuilder<T>(
                selected,
                t -> list.stream()
                        .filter(e -> t.equals(e.getEntry().getStore()))
                        .findFirst()
                        .orElseThrow()
                        .createRegion(),
                new Label(AppI18n.get("none")),
                n -> true);
        comboBox.setSelectedDisplay(t -> createGraphic(t));
        comboBox.setUnknownNode(t -> createGraphic(t));

        for (var e : list) {
            if (e.getEntry().getStore() == self) {
                continue;
            }

            var s = e.getEntry().getStore();
            if (!(mode == Mode.ENVIRONMENT) && s instanceof LeafShellStore) {
                continue;
            }

            var node = comboBox.add((T) e.getEntry().getStore());
            if (!storeClass.isAssignableFrom(s.getClass()) || !applicableCheck.test((T) s)) {
                comboBox.disable(node);
            }
        }

        ComboBox<Node> cb = comboBox.build();
        cb.getStyleClass().add("choice-comp");
        cb.setMaxWidth(2000);
        return cb;
    }
}
