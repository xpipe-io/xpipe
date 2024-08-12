package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.store.DataStore;
import javafx.beans.value.ObservableValue;

public class LaunchStoreAction implements ActionProvider {

    @Override
    public String getId() {
        return "launch";
    }

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<>() {

            @Override
            public boolean canLinkTo() {
                return true;
            }

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<DataStore> store) {
                return store.get().getProvider().launchAction(store.get());
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
                return o.get().getValidity().isUsable()
                        && (o.get().getProvider().launchAction(o.get()) != null);
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
                return AppI18n.observable("launch");
            }

            @Override
            public String getIcon(DataStoreEntryRef<DataStore> store) {
                return "mdi2p-play";
            }
        };
    }

    @Override
    public DefaultDataStoreCallSite<?> getDefaultDataStoreCallSite() {
        return new DefaultDataStoreCallSite<>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<DataStore> store) {
                return store.get().getProvider().launchAction(store.get());
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
                return o.get().getValidity().isUsable()
                        && (o.get().getProvider().launchAction(o.get()) != null);
            }
        };
    }
}
