package io.xpipe.ext.base.action;

import io.xpipe.app.core.AppActionLinkDetector;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.ClipboardHelper;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.InPlaceSecretValue;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class ShareStoreAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<>() {

            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<DataStore> store) {
                return new Action(store.get());
            }

            @Override
            public Class<DataStore> getApplicableClass() {
                return DataStore.class;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<DataStore> o) {
                return o.get().getProvider().isShareable(o.get());
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<DataStore> store) {
                return AppI18n.observable("base.copyShareLink");
            }

            @Override
            public String getIcon(DataStoreEntryRef<DataStore> store) {
                return "mdi2c-clipboard-list-outline";
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntry store;

        public static String create(DataStore store) {
            return "xpipe://addStore/" + InPlaceSecretValue.of(store.toString()).getEncryptedValue();
        }

        @Override
        public void execute() {
            var url = create(store.getStore());
            AppActionLinkDetector.setLastDetectedAction(url);
            ClipboardHelper.copyUrl(url);
        }
    }
}
