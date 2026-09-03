package io.xpipe.app.hub.category;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.mode.AppOperationMode;
import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.hub.list.StoreFilterState;
import io.xpipe.app.hub.list.StoreViewState;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreCategoryConfig;
import io.xpipe.app.storage.DataStoreColor;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableStringValue;

import lombok.Getter;
import org.int4.fx.values.util.Trigger;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
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
    private final DerivedObservableList<StoreCategoryWrapper> shownChildren;
    private final DerivedObservableList<StoreEntryWrapper> directContainedEntries;
    private final IntegerProperty shownContainedEntriesCount = new SimpleIntegerProperty();
    private final IntegerProperty allContainedEntriesCount = new SimpleIntegerProperty();
    private final DoubleProperty orderIndex = new SimpleDoubleProperty();
    private final BooleanProperty expanded = new SimpleBooleanProperty();
    private final Property<DataStoreColor> color = new SimpleObjectProperty<>();
    private final Property<String> iconFile = new SimpleObjectProperty<>();
    private final Trigger<Void> renameTrigger = Trigger.of();
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
        this.shownChildren = DerivedObservableList.arrayList(true);
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

    public boolean isHierarchyExpanded() {
        StoreCategoryWrapper current = this;
        while ((current = current.getParent()) != null) {
            if (!current.getExpanded().get()) {
                return false;
            }
        }
        return true;
    }

    public void select() {
        PlatformThread.runLaterIfNeeded(() -> {
            StoreViewState.get().getActiveCategory().setValue(this);
        });
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
            if (!category.getName().equals(name.getValue())) {
                Platform.runLater(() -> {
                    name.setValue(category.getName());
                });
            }
        });

        expanded.addListener((c, o, n) -> {
            category.setExpanded(n);
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

    public void updateConfig(DataStoreCategoryConfig config) {
        DataStorage.get().updateCategoryConfig(getCategory(), config);
        StoreViewState.get().updateWrappers();
    }

    public synchronized void update() {
        // We are probably in shutdown then
        if (AppOperationMode.isInShutdown() || StoreViewState.get() == null) {
            return;
        }

        // We received a delayed update after removal
        if (!DataStorage.get().getStoreCategories().contains(category)) {
            return;
        }

        // Avoid reupdating name when changed from the name property!
        var catName = translatedName(category.getName());
        if (!catName.equals(name.getValue())) {
            name.setValue(catName);
        }

        orderIndex.set(category.getOrderIndex());
        lastAccess.setValue(category.getLastAccess().minus(Duration.ofMillis(500)));
        sync.setValue(Boolean.TRUE.equals(
                DataStorage.get().getEffectiveCategoryConfig(category).getSync()));
        expanded.setValue(category.isExpanded());
        color.setValue(DataStorage.get().getEffectiveCategoryConfig(category).getColor());
        iconFile.setValue(category.getEffectiveIconFile());

        var allEntries = new ArrayList<>(StoreViewState.get().getAllEntries().getList());
        directContainedEntries.setContent(allEntries.stream()
                .filter(entry -> {
                    return entry.getEntry().getCategoryUuid().equals(category.getUuid());
                })
                .toList());

        var comparator = Comparator.<StoreCategoryWrapper>comparingDouble(
                value -> value.getCategory().getOrderIndex());
        children.setContent(StoreViewState.get().getCategories().getList().stream()
                .filter(storeCategoryWrapper -> getCategory()
                        .getUuid()
                        .equals(storeCategoryWrapper.getCategory().getParentCategory()))
                .sorted(comparator)
                .toList());
        shownChildren.setContent(children.getList().stream()
                .filter(wrapper -> {
                    var op = StoreViewState.get().getCategoryDragOperation().getValue();
                    return op == null || !wrapper.equals(op.getSelection());
                })
                .toList());
        var direct = directContainedEntries
                .getList()
                .filtered(storeEntryWrapper -> storeEntryWrapper.includeInConnectionCount())
                .size();
        var sub = children.getList().stream()
                .mapToInt(value -> value.allContainedEntriesCount.get())
                .sum();
        allContainedEntriesCount.setValue(direct + sub);

        var directFiltered = directContainedEntries.getList().stream()
                .filter(storeEntryWrapper -> {
                    var filter = StoreFilterState.get().getEffectiveFilter().getValue();
                    if (filter != null) {
                        var matches = storeEntryWrapper.matchesFilter(filter);
                        return matches;
                    }

                    return storeEntryWrapper.includeInConnectionCount();
                })
                .count();
        // Due to always including filtered entries, there is the possibility of exceeding the direct count
        directFiltered = Math.min(directFiltered, direct);
        var subFiltered = children.getList().stream()
                .mapToInt(value -> value.shownContainedEntriesCount.get())
                .sum();
        shownContainedEntriesCount.setValue(directFiltered + subFiltered);
        Optional.ofNullable(getParent()).ifPresent(storeCategoryWrapper -> {
            storeCategoryWrapper.update();
        });

        StoreViewState.get().refreshActiveCategory();
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
        if (original.equals("Sources")) {
            return AppI18n.get("sources");
        }

        return original;
    }

    public boolean canDrag() {
        return category.getParentCategory() != null && DataStorage.get().canMoveStoreCategory(category);
    }

    public boolean canMoveIntoThis(StoreCategoryWrapper wrapper) {
        if (wrapper.getParent() == null
                || wrapper.equals(this)
                || !wrapper.getRoot().equals(getRoot())) {
            return false;
        }

        var isSource = DataStorage.get().getCategoryParentHierarchy(category).stream()
                .anyMatch(h -> h.getUuid().equals(DataStorage.SCRIPT_SOURCES_CATEGORY_UUID));
        if (isSource) {
            return DataStorage.get().getCategoryParentHierarchy(wrapper.getCategory()).stream()
                    .anyMatch(h -> h.getUuid().equals(DataStorage.SCRIPT_SOURCES_CATEGORY_UUID));
        }

        var isScript = DataStorage.get().getCategoryParentHierarchy(category).stream()
                .anyMatch(h -> h.getUuid().equals(DataStorage.ALL_SCRIPTS_CATEGORY_UUID));
        if (isScript) {
            return DataStorage.get().getCategoryParentHierarchy(wrapper.getCategory()).stream()
                    .noneMatch(h -> h.getUuid().equals(DataStorage.SCRIPT_SOURCES_CATEGORY_UUID));
        }

        return true;
    }

    public boolean canMoveToThis(StoreEntryWrapper wrapper) {
        var sourceCat = DataStorage.get().getStoreCategory(wrapper.getEntry());
        if (category.getParentCategory() == null || sourceCat.equals(category)) {
            return false;
        }

        var targetHierarchy = DataStorage.get().getCategoryParentHierarchy(category);

        var isSource = DataStorage.get().getCategoryParentHierarchy(sourceCat).stream()
                .anyMatch(h -> h.getUuid().equals(DataStorage.SCRIPT_SOURCES_CATEGORY_UUID));
        if (isSource) {
            return targetHierarchy.stream().anyMatch(h -> h.getUuid().equals(DataStorage.SCRIPT_SOURCES_CATEGORY_UUID));
        }

        var isScript = DataStorage.get().getCategoryParentHierarchy(sourceCat).stream()
                .anyMatch(h -> h.getUuid().equals(DataStorage.ALL_SCRIPTS_CATEGORY_UUID));
        if (isScript) {
            return targetHierarchy.stream().anyMatch(h -> h.getUuid().equals(DataStorage.ALL_SCRIPTS_CATEGORY_UUID))
                    && targetHierarchy.stream()
                            .noneMatch(h -> h.getUuid().equals(DataStorage.SCRIPT_SOURCES_CATEGORY_UUID));
        }

        var isSpecialIdentity = wrapper.getEntry().getValidity().isUsable()
                && (wrapper.getEntry().getProvider().getId().equals("passwordManagerIdentity")
                        || wrapper.getEntry().getProvider().getId().equals("multiIdentity"));
        if (isSpecialIdentity) {
            return targetHierarchy.stream().anyMatch(h -> h.getUuid().equals(DataStorage.ALL_IDENTITIES_CATEGORY_UUID));
        }

        var isLocalIdentity = DataStorage.get().getCategoryParentHierarchy(sourceCat).stream()
                .anyMatch(h -> h.getUuid().equals(DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID));
        if (isLocalIdentity) {
            return targetHierarchy.stream().anyMatch(h -> h.getUuid().equals(DataStorage.ALL_IDENTITIES_CATEGORY_UUID))
                    && targetHierarchy.stream()
                            .noneMatch(h -> h.getUuid().equals(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID));
        }

        var isSyncedIdentity = DataStorage.get().getCategoryParentHierarchy(sourceCat).stream()
                .anyMatch(h -> h.getUuid().equals(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID));
        if (isSyncedIdentity) {
            return targetHierarchy.stream().anyMatch(h -> h.getUuid().equals(DataStorage.ALL_IDENTITIES_CATEGORY_UUID))
                    && targetHierarchy.stream()
                            .noneMatch(h -> h.getUuid().equals(DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID));
        }

        var isConnection = DataStorage.get().getCategoryParentHierarchy(sourceCat).stream()
                .anyMatch(h -> h.getUuid().equals(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID));
        if (isConnection) {
            return targetHierarchy.stream()
                    .anyMatch(h -> h.getUuid().equals(DataStorage.ALL_CONNECTIONS_CATEGORY_UUID));
        }

        return false;
    }

    public void insertSiblingCategory(StoreCategoryWrapper selection, boolean after) {
        var l = getParent().getShownChildren().getList();
        var index = l.indexOf(this);
        var min = index > 0 || after
                ? l.get(index - (after ? 0 : 1)).getCategory().getOrderIndex()
                : l.getFirst().getCategory().getOrderIndex() - 1.0;
        var max = index < l.size() - 1 || !after
                ? l.get(index + (after ? 1 : 0)).getCategory().getOrderIndex()
                : l.getLast().getCategory().getOrderIndex() + 1.0;
        var inc = Math.min((max - min) / 2.0, 1.0);

        DataStorage.get()
                .moveCategoryToParent(selection.getCategory(), getParent().getCategory());
        selection.getCategory().setOrderIndex(min + inc);

        update();
        selection.update();
    }

    public void insertSubCategory(StoreCategoryWrapper selection) {
        var l = getShownChildren().getList();
        if (l.isEmpty()) {
            DataStorage.get().moveCategoryToParent(selection.getCategory(), getCategory());
            return;
        }

        var first = l.getFirst();
        first.insertSiblingCategory(first, false);
    }

    public Property<String> nameProperty() {
        return name;
    }
}
