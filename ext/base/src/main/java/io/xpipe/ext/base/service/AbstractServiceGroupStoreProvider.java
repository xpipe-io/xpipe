package io.xpipe.ext.base.service;

import io.xpipe.app.comp.BaseRegionBuilder;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.CountGroupStoreProvider;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreProvider;
import io.xpipe.app.ext.DataStoreUsageCategory;
import io.xpipe.app.hub.comp.*;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.DocumentationLink;

import javafx.beans.binding.Bindings;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.value.ObservableValue;

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
    public DataStoreEntry getDisplayParent(DataStoreEntry store) {
        AbstractServiceGroupStore<?> s = store.getStore().asNeeded();
        return s.getParent().get();
    }

    public String getDisplayIconFileName(DataStore store) {
        return "base:serviceGroup_icon.svg";
    }
}
