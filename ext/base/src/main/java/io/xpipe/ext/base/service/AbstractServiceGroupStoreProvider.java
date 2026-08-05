package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.CountGroupStoreProvider;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.store.DataStoreUsageCategory;
import io.xpipe.app.util.DocumentationLink;

public abstract class AbstractServiceGroupStoreProvider implements CountGroupStoreProvider {

    @Override
    public String getCountTranslationKey() {
        return "Service";
    }

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.SERVICES;
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.GROUP;
    }

    @Override
    public DataStoreEntryRef<?> getDisplayParent(DataStoreEntry store) {
        AbstractServiceGroupStore<?> s = store.getStore().asNeeded();
        return s.getParent();
    }

    public String getDisplayIconFileName(DataStore store) {
        return "base:serviceGroup_icon.svg";
    }
}
