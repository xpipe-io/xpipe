package io.xpipe.ext.base.service;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.action.LeafStoreActionProvider;
import io.xpipe.app.action.StoreAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ClipboardHelper;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class ServiceCopyAddressActionProvider implements LeafStoreActionProvider<AbstractServiceStore> {

            @Override
            public boolean isMajor(DataStoreEntryRef<AbstractServiceStore> o) {
                return true;
            }

            @Override
            public AbstractAction createAction(DataStoreEntryRef<AbstractServiceStore> ref) {
                return Action.builder().ref(ref).build();
            }

            @Override
            public Class<AbstractServiceStore> getApplicableClass() {
                return AbstractServiceStore.class;
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
    public String getId() {
        return "copyServiceAddress";
    }
@Jacksonized
@SuperBuilder
    static class Action extends StoreAction<AbstractServiceStore> {

        @Override
        public void executeImpl() throws Exception {
            var serviceStore = ref.getStore();
            serviceStore.startSessionIfNeeded();
            var l = serviceStore.requiresTunnel()
                    ? serviceStore.getSession().getLocalPort()
                    : serviceStore.getRemotePort();
            var base = "localhost:" + l;
            var full = serviceStore.getServiceProtocolType().formatAddress(base);
            ClipboardHelper.copyUrl(full);
        }
    }
}
