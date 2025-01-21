package io.xpipe.ext.base.service;

import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;

import lombok.Value;

public class ServiceOpenAction implements ActionProvider {

    @Override
    public DefaultDataStoreCallSite<?> getDefaultDataStoreCallSite() {
        return new DefaultDataStoreCallSite<AbstractServiceStore>() {
            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<AbstractServiceStore> store) {
                return new Action(store.getStore());
            }

            @Override
            public Class<AbstractServiceStore> getApplicableClass() {
                return AbstractServiceStore.class;
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
            serviceStore.getServiceProtocolType().open(full);
        }
    }
}
