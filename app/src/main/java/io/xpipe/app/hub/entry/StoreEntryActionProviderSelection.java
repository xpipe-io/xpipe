package io.xpipe.app.hub.entry;

import io.xpipe.app.action.ActionProvider;
import io.xpipe.app.hub.action.HubBranchProvider;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.HubMenuItemProvider;
import io.xpipe.app.hub.action.impl.EditHubLeafProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.store.GroupStore;

import lombok.Value;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

@Value
public class StoreEntryActionProviderSelection {

    ActionProvider defaultProvider;
    List<? extends HubMenuItemProvider<?>> majorProviders;
    List<? extends HubMenuItemProvider<?>> minorProviders;

    public static StoreEntryActionProviderSelection of(DataStoreEntry entry, StoreEntryActionProviderSelectionState state) {
        if (state.isTemplate()) {
            return new StoreEntryActionProviderSelection(new EditHubLeafProvider(), List.of(), List.of());
        }

        var defaultProvider = ActionProvider.ALL.stream()
                .filter(e -> entry.getStore() != null
                        && e instanceof HubLeafProvider<?> def
                        && (entry.getValidity().isUsable()
                                || (!def.requiresValidStore() && entry.getProvider() != null))
                        && def.getApplicableClass()
                                .isAssignableFrom(entry.getStore().getClass())
                        && def.isApplicable(entry.ref())
                        && def.isDefault())
                .findFirst()
                .or(() -> {
                    if (entry.getStore() instanceof GroupStore<?>) {
                        return Optional.empty();
                    } else if (entry.getProvider() != null
                            && entry.getProvider().canConfigure()) {
                        return Optional.of(new EditHubLeafProvider());
                    } else {
                        return Optional.empty();
                    }
                })
                .orElse(null);

        var majorProviders = ActionProvider.ALL.stream()
                .map(actionProvider -> actionProvider instanceof HubMenuItemProvider<?> sa ? sa : null)
                .filter(Objects::nonNull)
                .filter(dataStoreActionProvider ->
                        showActionProvider(entry, state.isCompact(), dataStoreActionProvider, true))
                .toList();

        var minorProviders = ActionProvider.ALL.stream()
                .map(actionProvider -> actionProvider instanceof HubMenuItemProvider<?> sa ? sa : null)
                .filter(Objects::nonNull)
                .filter(dataStoreActionProvider ->
                        showActionProvider(entry, state.isCompact(), dataStoreActionProvider, false))
                .collect(Collectors.toCollection(ArrayList::new));
        minorProviders.removeIf(storeActionProvider -> {
            return majorProviders.stream().anyMatch(mj -> {
                return mj instanceof HubBranchProvider<?> branch
                        && branch.getChildren(entry.ref()).stream()
                                .anyMatch(c -> c.getClass().equals(storeActionProvider.getClass()));
            });
        });

        return new StoreEntryActionProviderSelection(defaultProvider, majorProviders, minorProviders);
    }

    public static boolean showActionProvider(DataStoreEntry entry, boolean compact, ActionProvider p, boolean major) {
        if (p instanceof HubLeafProvider<?> leaf) {
            return (entry.getValidity().isUsable() || (!leaf.requiresValidStore() && entry.getProvider() != null))
                    && leaf.getApplicableClass()
                            .isAssignableFrom(entry.getStore().getClass())
                    && leaf.isApplicable(entry.ref())
                    && ((!compact && major == leaf.isMajor()) || (compact && !major));
        }

        if (p instanceof HubBranchProvider<?> branch
                && entry.getStore() != null
                && branch.getApplicableClass().isAssignableFrom(entry.getStore().getClass())
                && branch.isApplicable(entry.ref())
                && ((!compact && major == branch.isMajor()) || (compact && !major))) {
            return branch.getChildren(entry.ref()).stream()
                    .anyMatch(child -> showActionProvider(entry, compact, child, false));
        }

        return false;
    }
}
