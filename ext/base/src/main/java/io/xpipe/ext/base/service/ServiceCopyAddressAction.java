package io.xpipe.ext.base.service;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ClipboardHelper;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class ServiceCopyAddressAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<AbstractServiceStore>() {

            @Override
            public boolean isMajor(DataStoreEntryRef<AbstractServiceStore> o) {
                return true;
            }

            @Override
            public boolean canLinkTo() {
                return true;
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<AbstractServiceStore> store) {
                return new Action(store.getStore());
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
            public String getIcon(DataStoreEntryRef<AbstractServiceStore> store) {
                return "mdi2c-content-copy";
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        AbstractServiceStore serviceStore;

        @Override
        public void execute() throws Exception {
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
