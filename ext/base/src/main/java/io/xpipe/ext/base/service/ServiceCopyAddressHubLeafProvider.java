package io.xpipe.ext.base.service;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ClipboardHelper;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ServiceCopyAddressHubLeafProvider implements HubLeafProvider<AbstractServiceStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public boolean isMajor(DataStoreEntryRef<AbstractServiceStore> o) {
        return true;
    }

    @Override
    public ObservableValue<String> getName(DataStoreEntryRef<AbstractServiceStore> store) {
        return AppI18n.observable("copyAddress");
    }

    @Override
    public LabelGraphic getIcon(DataStoreEntryRef<AbstractServiceStore> store) {
        return new LabelGraphic.IconGraphic("mdi2c-content-copy");
    }

    @Override
    public Class<AbstractServiceStore> getApplicableClass() {
        return AbstractServiceStore.class;
    }

    @Override
    public AbstractAction createAction(DataStoreEntryRef<AbstractServiceStore> ref) {
        return Action.builder().ref(ref).build();
    }

    @Override
    public String getId() {
        return "copyServiceAddress";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<AbstractServiceStore> {

        @Override
        public void executeImpl() throws Exception {
            var serviceStore = ref.getStore();
            serviceStore.startSessionIfNeeded();
            var full = serviceStore.getServiceProtocolType().formatAddress(serviceStore.getOpenTargetUrl());
            ClipboardHelper.copyUrl(full);
        }
    }
}
