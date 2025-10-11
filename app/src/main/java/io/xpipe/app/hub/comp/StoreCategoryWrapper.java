package io.xpipe.app.hub.comp;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreColor;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableStringValue;

import lombok.Getter;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Optional;

@Getter
public class StoreCategoryWrapper {

    private final DataStoreCategory root;
    private final int depth;
    private final Property<String> name;
    private final DataStoreCategory category;
    private final Property<Instant> lastAccess;
    private final BooleanProperty sync;
    private final DerivedObservableList<StoreCategoryWrapper> children;
    private final DerivedObservableList<StoreEntryWrapper> directContainedEntries;
    private final IntegerProperty shownContainedEntriesCount = new SimpleIntegerProperty();
    private final IntegerProperty allContainedEntriesCount = new SimpleIntegerProperty();
    private final BooleanProperty expanded = new SimpleBooleanProperty();
    private final Property<DataStoreColor> color = new SimpleObjectProperty<>();
    private final BooleanProperty largeCategoryOptimizations = new SimpleBooleanProperty();
    private StoreCategoryWrapper cachedParent;

    public StoreCategoryWrapper(DataStoreCategory category) {
        var d = 0;
        DataStoreCategory last = category;
        DataStoreCategory p = category;
        while ((p = DataStorage.get()
                        .getStoreCategoryIfPresent(p.getParentCategory())
                        .orElse(null))
                != null) {
            d++;
            last = p;
        }
        depth = d;

        this.root = last;
        this.category = category;
        this.name = new SimpleStringProperty(category.getName());
        this.lastAccess = new SimpleObjectProperty<>(category.getLastAccess());
        this.sync = new SimpleBooleanProperty(Boolean.TRUE.equals(
                DataStorage.get().getEffectiveCategoryConfig(category).getSync()));
        this.children = DerivedObservableList.arrayList(true);
        this.directContainedEntries = DerivedObservableList.arrayList(true);
        this.color.setValue(
                DataStorage.get().getEffectiveCategoryConfig(category).getColor());
        setupListeners();
    }

    public ObservableStringValue getShownName() {
        return Bindings.createStringBinding(
                () -> {
                    var n = nameProperty().getValue();
                    return AppPrefs.get().censorMode().get() ? "*".repeat(n.length()) : n;
                },
                AppPrefs.get().censorMode(),
                nameProperty());
    }

    public StoreCategoryWrapper getRoot() {
        return StoreViewState.get().getCategoryWrapper(root);
    }

    public StoreCategoryWrapper getParent() {
        if (category.getParentCategory() == null) {
            return null;
        }

        if (cachedParent == null) {
            cachedParent = StoreViewState.get().getCategories().getList().stream()
                    .filter(storeCategoryWrapper ->
                            storeCategoryWrapper.getCategory().getUuid().equals(category.getParentCategory()))
                    .findAny()
                    .orElse(null);
        }

        return cachedParent;
    }

    public void select() {
        PlatformThread.runLaterIfNeeded(() -> {
            StoreViewState.get().getActiveCategory().setValue(this);
        });
    }

    public void moveToParent(DataStoreCategory newParent) {
        DataStorage.get().moveCategoryToParent(category, newParent);
    }

    public void delete() {
        for (var c : children.getList()) {
            c.delete();
        }
        DataStorage.get().deleteStoreCategory(category, false, false);
    }

    private void setupListeners() {
        name.addListener((c, o, n) -> {
            if (n.equals(translatedName(category.getName()))) {
                return;
            }

            category.setName(n);
        });

        category.addListener(() -> PlatformThread.runLaterIfNeeded(() -> {
            update();
        }));

        AppPrefs.get().showChildCategoriesInParentCategory().addListener((observable, oldValue, newValue) -> {
            update();
        });

        AppI18n.activeLanguage().addListener((observable, oldValue, newValue) -> {
            update();
        });
    }

    public void toggleExpanded() {
        this.expanded.set(!expanded.getValue());
    }

    public void update() {
        // We are probably in shutdown then
        if (StoreViewState.get() == null) {
            return;
        }

        // Avoid reupdating name when changed from the name property!
        var catName = translatedName(category.getName());
        if (!catName.equals(name.getValue())) {
            name.setValue(catName);
        }

        lastAccess.setValue(category.getLastAccess().minus(Duration.ofMillis(500)));
        sync.setValue(Boolean.TRUE.equals(
                DataStorage.get().getEffectiveCategoryConfig(category).getSync()));
        expanded.setValue(category.isExpanded());
        color.setValue(DataStorage.get().getEffectiveCategoryConfig(category).getColor());

        var allEntries = new ArrayList<>(StoreViewState.get().getAllEntries().getList());
        directContainedEntries.setContent(allEntries.stream()
                .filter(entry -> {
                    return entry.getEntry().getCategoryUuid().equals(category.getUuid());
                })
                .toList());

        children.setContent(StoreViewState.get().getCategories().getList().stream()
                .filter(storeCategoryWrapper -> getCategory()
                        .getUuid()
                        .equals(storeCategoryWrapper.getCategory().getParentCategory()))
                .toList());
        var direct = directContainedEntries
                .getList()
                .filtered(storeEntryWrapper -> storeEntryWrapper.includeInConnectionCount())
                .size();
        var sub = children.getList().stream()
                .mapToInt(value -> value.allContainedEntriesCount.get())
                .sum();
        allContainedEntriesCount.setValue(direct + sub);

        var performanceCount =
                AppPrefs.get().showChildCategoriesInParentCategory().get() ? allContainedEntriesCount.get() : direct;
        if (performanceCount > 500) {
            largeCategoryOptimizations.setValue(true);
        }

        var directFiltered = directContainedEntries.getList().stream()
                .filter(storeEntryWrapper -> storeEntryWrapper.includeInConnectionCount()
                        && storeEntryWrapper.matchesFilter(
                                StoreViewState.get().getFilterString().getValue()))
                .count();
        var subFiltered = children.getList().stream()
                .mapToInt(value -> value.shownContainedEntriesCount.get())
                .sum();
        shownContainedEntriesCount.setValue(directFiltered + subFiltered);
        Optional.ofNullable(getParent()).ifPresent(storeCategoryWrapper -> {
            storeCategoryWrapper.update();
        });
    }

    private String translatedName(String original) {
        if (original.equals("All connections")) {
            return AppI18n.get("allConnections");
        }
        if (original.equals("All scripts")) {
            return AppI18n.get("allScripts");
        }
        if (original.equals("All identities")) {
            return AppI18n.get("allIdentities");
        }
        if (original.equals("All macros")) {
            return AppI18n.get("allMacros");
        }
        if (original.equals("Local")) {
            return AppI18n.get("local");
        }
        if (original.equals("Synced")) {
            return AppI18n.get("synced");
        }
        if (original.equals("Predefined") || original.equals("Samples")) {
            return AppI18n.get("samples");
        }
        if (original.equals("Custom")) {
            return AppI18n.get("custom");
        }
        if (original.equals("Default")) {
            return AppI18n.get("default");
        }

        return original;
    }

    public Property<String> nameProperty() {
        return name;
    }
}
