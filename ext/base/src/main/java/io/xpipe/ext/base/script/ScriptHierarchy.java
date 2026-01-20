package io.xpipe.ext.base.script;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import lombok.Value;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.function.Predicate;

@Value
public class ScriptHierarchy {

    DataStoreEntryRef<?> base;
    List<ScriptHierarchy> children;

    public static ScriptHierarchy buildEnabledHierarchy(Predicate<DataStoreEntryRef<ScriptStore>> include) {
        var enabled = ScriptStoreSetup.getEnabledScripts();
        var all = new HashSet<DataStoreEntryRef<?>>(ScriptStoreSetup.getEnabledScripts());

        // Add parents
        for (DataStoreEntryRef<ScriptStore> ref : enabled) {
            DataStoreEntryRef<?> current = ref;
            while (true) {
                var parent = DataStorage.get().getDefaultDisplayParent(current.get());
                if (parent.isPresent()) {
                    DataStoreEntryRef<ScriptGroupStore> next = parent.get().ref();
                    all.add(next);
                    current = next;
                } else {
                    break;
                }
            }
        }

        var top = all.stream()
                .filter(ref -> {
                    var parent = DataStorage.get().getDefaultDisplayParent(ref.get());
                    return parent.isEmpty();
                })
                .toList();

        var mapped = top.stream()
                .map(ref -> buildHierarchy(ref, check -> {
                    if (!include.test(check.asNeeded())) {
                        return false;
                    }

                    return all.contains(check);
                }))
                .map(hierarchy -> condenseHierarchy(hierarchy))
                .filter(hierarchy -> hierarchy.show())
                .sorted(Comparator.comparing(scriptHierarchy ->
                        scriptHierarchy.getBase().get().getName().toLowerCase()))
                .toList();
        return condenseHierarchy(new ScriptHierarchy(null, mapped));
    }

    private static ScriptHierarchy buildHierarchy(
            DataStoreEntryRef<?> ref, Predicate<DataStoreEntryRef<ScriptStore>> include) {
        if (ref.getStore() instanceof ScriptGroupStore) {
            var directChildren = DataStorage.get().getStoreChildren(ref.get()).stream()
                    .filter(entry -> entry.getValidity().isUsable() && entry.getStore() instanceof ScriptStore)
                    .map(dataStoreEntry -> dataStoreEntry.<ScriptStore>ref())
                    .toList();
            var children = directChildren.stream()
                    .filter(include)
                    .map(c -> buildHierarchy(c, include))
                    .filter(hierarchy -> hierarchy.show())
                    .sorted(Comparator.comparing(scriptHierarchy ->
                            scriptHierarchy.getBase().get().getName().toLowerCase()))
                    .toList();
            return new ScriptHierarchy(ref, children);
        } else {
            return new ScriptHierarchy(ref, List.of());
        }
    }

    public static ScriptHierarchy condenseHierarchy(ScriptHierarchy hierarchy) {
        var children =
                hierarchy.getChildren().stream().map(c -> condenseHierarchy(c)).toList();
        if (children.size() == 1 && !children.getFirst().isLeaf()) {
            var nestedChildren = children.getFirst().getChildren();
            return new ScriptHierarchy(hierarchy.getBase(), nestedChildren);
        } else {
            return new ScriptHierarchy(hierarchy.getBase(), children);
        }
    }

    public boolean show() {
        return isLeaf() || !isEmptyBranch();
    }

    public boolean isEmptyBranch() {
        return (base == null || base.getStore() instanceof ScriptGroupStore) && children.isEmpty();
    }

    public boolean isLeaf() {
        return base != null && base.getStore() instanceof ScriptStore && children.isEmpty();
    }

    public DataStoreEntryRef<ScriptStore> getLeafBase() {
        return base.asNeeded();
    }
}
