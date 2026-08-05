package io.xpipe.ext.base.identity;

import io.xpipe.app.hub.entry.StoreEntryBadge;
import io.xpipe.app.hub.entry.StoreEntryInformation;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.identity.NoIdentityStrategy;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.store.DataStoreCreationCategory;
import io.xpipe.app.store.DataStoreProvider;
import io.xpipe.app.store.DataStoreUsageCategory;
import io.xpipe.app.util.DocumentationLink;

import java.util.List;

public abstract class IdentityStoreProvider implements DataStoreProvider {

    @Override
    public DataStoreEntryRef<?> getDisplayParent(DataStoreEntry store) {
        return MultiIdentityStore.getExclusiveHolder(store.ref()).orElse(null);
    }

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.IDENTITIES;
    }

    @Override
    public List<String> getSearchableTerms(DataStore store) {
        IdentityStore s = store.asNeeded();
        var name = s.getUsername().getFixedUsername();
        return name.isPresent() ? List.of(name.get()) : List.of();
    }

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStoreCreationCategory.IDENTITY;
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.IDENTITY;
    }

    @Override
    public StoreEntryInformation buildInformation(StoreSection section) {
        var st = (IdentityStore) section.getWrapper().getStore().getValue();
        var user =
                st.getUsername().hasUser() ? st.getUsername().getFixedUsername().orElse(null) : "Anonymous user";
        var password = st.getPassword() == null || st.getPassword() instanceof SecretNoneStrategy ? null : "Password";
        var key = st.getSshIdentity() == null || st.getSshIdentity() instanceof NoIdentityStrategy ? null : "Key";
        return StoreEntryInformation.of(
                StoreEntryBadge.ofUser(user), StoreEntryBadge.ofPassword(password), StoreEntryBadge.ofKey(key));
    }
}
