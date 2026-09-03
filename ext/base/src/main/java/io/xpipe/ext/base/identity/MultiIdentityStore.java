package io.xpipe.ext.base.identity;

import io.xpipe.app.identity.SshIdentityStrategy;
import io.xpipe.app.identity.UsernameStrategy;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreAccessScope;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.store.StatefulDataStore;
import io.xpipe.app.util.ValidationException;
import io.xpipe.app.util.Validators;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@SuperBuilder
@JsonTypeName("multiIdentity")
@Jacksonized
@Value
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
public class MultiIdentityStore extends IdentityStore implements StatefulDataStore<MultiIdentityStoreState> {

    public static boolean isExclusivelyHeld(DataStoreEntryRef<IdentityStore> ref) {
        return getExclusiveHolder(ref).isPresent();
    }

    public static Optional<DataStoreEntryRef<MultiIdentityStore>> getExclusiveHolder(
            DataStoreEntryRef<IdentityStore> ref) {
        var exclusiveHolder = DataStorage.get().getStoreEntries().stream()
                .filter(entry -> entry.getValidity().isUsable()
                        && entry.getStore() instanceof MultiIdentityStore m
                        && m.getExclusive() != null
                        && m.getExclusive()
                        && m.getAvailableIdentities().contains(ref))
                .findFirst();
        return exclusiveHolder.map(entry -> entry.ref());
    }

    List<UUID> identities;
    Boolean exclusive;
    DataStoreAccessScope accessScope;

    public DataStoreAccessScope getAccessScopeRaw() {
        return accessScope;
    }

    @Override
    public DataStoreAccessScope getAccessScope() {
        return accessScope != null ? accessScope : DataStoreAccessScope.encryption();
    }

    @Override
    public String toSummary() {
        var selected = getSelected();
        if (selected.isPresent()) {
            return selected.get().getStore().toSummary() + " ["
                    + selected.get().get().getName() + "]";
        } else {
            return "?";
        }
    }

    @Override
    public String getName() {
        return getSelected().map(ref -> ref.getStore().getName()).orElse(null);
    }

    public List<DataStoreEntryRef<IdentityStore>> getAvailableIdentities() {
        return identities.stream()
                .map(uuid -> DataStorage.get().getStoreEntryIfPresent(uuid))
                .flatMap(Optional::stream)
                .map(e -> e.<IdentityStore>ref())
                .filter(ref -> ref != null && ref.get().getValidity().isUsable())
                .toList();
    }

    public Optional<DataStoreEntryRef<IdentityStore>> getSelected() {
        var cached = getState().getSelected();
        if (cached != null) {
            var entry = DataStorage.get().getStoreEntryIfPresent(cached);
            if (entry.isPresent()
                    && entry.get().getValidity().isUsable()
                    && getAvailableIdentities().contains(entry.get().ref())) {
                return Optional.of(entry.get().ref());
            }
        }

        var fallback = getAvailableIdentities().stream().findFirst();
        if (fallback.isPresent()) {
            return fallback;
        }

        return Optional.empty();
    }

    public void select(DataStoreEntryRef<IdentityStore> entry) {
        if (!getAvailableIdentities().contains(entry)) {
            return;
        }

        setState(MultiIdentityStoreState.builder()
                .selected(entry.get().getUuid())
                .build());
        DataStorage.get().finalizeWithDependencies(getSelfEntry());
    }

    private DataStoreEntryRef<IdentityStore> getSelectedOrThrow() {
        var found = getSelected();
        if (found.isPresent()) {
            return found.get();
        }
        throw ErrorEventFactory.expected(new IllegalStateException(
                "No available identity for multi identity " + getSelfEntry().getName()));
    }

    public boolean hasNestedMultiIdentities() {
        return getAvailableIdentities().stream().anyMatch(ref -> ref.getStore() instanceof MultiIdentityStore);
    }

    public boolean areAllIdentitiesAccessible() {
        return getAvailableIdentities().size() == identities.size();
    }

    public boolean areAnyChildrenLocal() {
        var childrenLocal = !getAvailableIdentities().isEmpty()
                && getAvailableIdentities().stream().anyMatch(ref -> {
                    return DataStorage.get().isInLocalIdentities(ref.get());
                });
        return childrenLocal;
    }

    public boolean areAllChildrenLocal() {
        var childrenLocal = !getAvailableIdentities().isEmpty()
                && getAvailableIdentities().stream().allMatch(ref -> {
                    return DataStorage.get().isInLocalIdentities(ref.get());
                });
        return childrenLocal;
    }

    public boolean areAllChildrenSynced() {
        var childrenSynced = !getAvailableIdentities().isEmpty()
                && getAvailableIdentities().stream().allMatch(ref -> {
                    return DataStorage.get().isInSyncedIdentities(ref.get());
                });
        return childrenSynced;
    }

    @Override
    public List<DataStoreEntryRef<?>> getDependencies() {
        return getAvailableIdentities().stream()
                .<DataStoreEntryRef<?>>map(DataStoreEntryRef::asNeeded)
                .toList();
    }

    @Override
    public void checkComplete() throws ValidationException {
        Validators.nonNull(getSelected().orElse(null));
    }

    @Override
    public DataStoreEntryRef<IdentityStore> getCustomEditTarget() {
        return getSelected().orElse(null);
    }

    public UsernameStrategy getUsername() {
        return getSelectedOrThrow().getStore().getUsername();
    }

    @Override
    public SecretRetrievalStrategy getPassword() {
        return getSelectedOrThrow().getStore().getPassword();
    }

    @Override
    public SshIdentityStrategy getSshIdentity() {
        return getSelectedOrThrow().getStore().getSshIdentity();
    }

    @Override
    public Class<MultiIdentityStoreState> getStateClass() {
        return MultiIdentityStoreState.class;
    }

    @Override
    public DataStore withUpdatedPrincipals() {
        var targetScope = DataStoreAccessScope.getTargetScope(accessScope);
        if (targetScope != null && targetScope.equals(DataStoreAccessScope.vault())) {
            targetScope = null;
        }

        return MultiIdentityStore.builder()
                .identities(identities)
                .exclusive(exclusive)
                .accessScope(targetScope)
                .build();
    }
}
