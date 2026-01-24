package io.xpipe.ext.base.script;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreCategory;
import io.xpipe.app.storage.DataStoreEntryRef;

import lombok.Value;

import java.util.*;
import java.util.function.Predicate;

@Value
public class ScriptHierarchy {

    String name;
    DataStoreCategory category;
    DataStoreEntryRef<ScriptStore> script;
    List<ScriptHierarchy> children;

    public static ScriptHierarchy buildEnabledHierarchy(Predicate<DataStoreEntryRef<ScriptStore>> include) {
        var enabled = ScriptStoreSetup.getEnabledScripts().stream().filter(include).toList();

        var categories = new HashSet<DataStoreCategory>();
        for (DataStoreEntryRef<ScriptStore> ref : enabled) {
            var cat = DataStorage.get().getStoreCategory(ref.get());
            var catParents = DataStorage.get().getCategoryParentHierarchy(cat);
            categories.addAll(catParents);
        }

        var hierarchy = new ScriptHierarchy(null, null, null, new ArrayList<>());
        while (true) {
            var changed = false;
            for (DataStoreCategory cat : categories) {
                // We don't support the All Scripts root
                if (cat.getParentCategory() == null) {
                    continue;
                }

                var toAdd = new ScriptHierarchy(cat.getName(), cat, null, new ArrayList<>());

                if (cat.getParentCategory().equals(DataStorage.ALL_SCRIPTS_CATEGORY_UUID)) {
                    if (!hierarchy.getChildren().contains(toAdd)) {
                        hierarchy.getChildren().add(toAdd);
                        changed = true;
                    }
                    continue;
                }

                var parentHierarchy = findParent(hierarchy, cat);
                if (parentHierarchy.isEmpty()) {
                    continue;
                }

                var alreadyAdded = parentHierarchy.get().getChildren().contains(toAdd);
                if (alreadyAdded) {
                    continue;
                }

                parentHierarchy.get().getChildren().add(toAdd);
                changed = true;
            }

            if (!changed) {
                break;
            }
        }

        for (DataStoreEntryRef<ScriptStore> scriptRef : enabled) {
            var scriptCategory = DataStorage.get().getStoreCategory(scriptRef.get());
            var catHierarchy = findParent(hierarchy, scriptCategory);
            if (catHierarchy.isEmpty()) {
                continue;
            }

            var childTarget = catHierarchy.get().getChildren().stream()
                    .filter(child -> child.getCategory().equals(scriptCategory))
                    .findFirst();
            if (childTarget.isEmpty()) {
                continue;
            }

            childTarget.get().getChildren().add(new ScriptHierarchy(scriptRef.get().getName(), null, scriptRef, List.of()));
        }

        return condenseHierarchy(hierarchy);
    }

    private static Optional<ScriptHierarchy> findParent(ScriptHierarchy hierarchy, DataStoreCategory category) {
        if (category.equals(hierarchy.getCategory())) {
            return Optional.of(hierarchy);
        }

        if (hierarchy.getChildren().stream().anyMatch(child -> category.equals(child.getCategory()))) {
            return Optional.of(hierarchy);
        }

        var children = hierarchy.getChildren();
        for (ScriptHierarchy child : children) {
            var foundInChild = findParent(child, category);
            if (foundInChild.isPresent()) {
                return foundInChild;
            }
        }

        return Optional.empty();
    }

    public static ScriptHierarchy condenseHierarchy(ScriptHierarchy hierarchy) {
        var children =
                hierarchy.getChildren().stream().map(c -> condenseHierarchy(c)).toList();
        if (children.size() == 1 && !children.getFirst().isLeaf()) {
            var nestedChildren = children.getFirst().getChildren();
            return new ScriptHierarchy(children.getFirst().getName(), hierarchy.getCategory(), hierarchy.getScript(), nestedChildren);
        } else {
            return new ScriptHierarchy(hierarchy.getName(), hierarchy.getCategory(), hierarchy.getScript(), children);
        }
    }

    public boolean show() {
        return isLeaf() || !isEmptyBranch();
    }

    public boolean isEmptyBranch() {
        if (category == null) {
            return false;
        }

        return children.isEmpty();
    }

    public boolean isLeaf() {
        return script != null;
    }
}
