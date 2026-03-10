package io.xpipe.ext.base.identity;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.cred.SshIdentityStrategy;
import io.xpipe.app.cred.UsernameStrategy;
import io.xpipe.app.ext.DataStoreDependencies;
import io.xpipe.app.ext.InternalCacheDataStore;
import io.xpipe.app.ext.StatefulDataStore;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.secret.EncryptedValue;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.Validators;
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

    List<UUID> identities;

    public List<DataStoreEntryRef<IdentityStore>> getAvailableIdentities() {
        return identities.stream().map(uuid -> DataStorage.get().getStoreEntryIfPresent(uuid)).flatMap(Optional::stream)
                .map(e -> e.<IdentityStore>ref())
                .filter(ref -> ref != null && ref.get().getValidity().isUsable()).toList();
    }

    public Optional<DataStoreEntryRef<IdentityStore>> getSelected() {
        var cached = getState().getSelected();
        if (cached != null) {
            var entry = DataStorage.get().getStoreEntryIfPresent(cached);
            if (entry.isPresent() && entry.get().getValidity().isUsable() && getAvailableIdentities().contains(entry.get().ref())) {
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

        setState(MultiIdentityStoreState.builder().selected(entry.get().getUuid()).build());
        DataStorage.get().finalizeWithDependencies(getSelfEntry());
    }

    private DataStoreEntryRef<IdentityStore> getSelectedOrThrow() {
        var found = getSelected();
        if (found.isPresent()) {
            return found.get();
        }
        throw ErrorEventFactory.expected(new IllegalStateException("No available identity for multi identity " + getSelfEntry().getName()));
    }

    @Override
    public List<DataStoreEntryRef<?>> getDependencies() {
        return DataStoreDependencies.of(getAvailableIdentities());
    }

    @Override
    public void checkComplete() throws Throwable {
        getSelectedOrThrow();
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
}
