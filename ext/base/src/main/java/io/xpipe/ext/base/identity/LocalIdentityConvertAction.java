package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.store.StoreCreationDialog;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ActionProvider;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.EncryptedValue;

import javafx.beans.value.ObservableValue;

import lombok.Value;

public class LocalIdentityConvertAction implements ActionProvider {

    @Override
    public LeafDataStoreCallSite<?> getLeafDataStoreCallSite() {
        return new LeafDataStoreCallSite<LocalIdentityStore>() {
            @Override
            public ActionProvider.Action createAction(DataStoreEntryRef<LocalIdentityStore> store) {
                return new Action(store);
            }

            @Override
            public ObservableValue<String> getName(DataStoreEntryRef<LocalIdentityStore> store) {
                return AppI18n.observable("sync");
            }

            @Override
            public boolean isMajor(DataStoreEntryRef<LocalIdentityStore> o) {
                return true;
            }

            @Override
            public String getIcon(DataStoreEntryRef<LocalIdentityStore> store) {
                return "mdi2g-git";
            }

            @Override
            public Class<?> getApplicableClass() {
                return LocalIdentityStore.class;
            }

            @Override
            public boolean isApplicable(DataStoreEntryRef<LocalIdentityStore> o) {
                return DataStorage.get().supportsSharing();
            }
        };
    }

    @Value
    static class Action implements ActionProvider.Action {

        DataStoreEntryRef<LocalIdentityStore> ref;

        @Override
        public void execute() {
            var st = ref.getStore();
            var synced = SyncedIdentityStore.builder()
                    .username(st.getUsername())
                    .password(EncryptedValue.VaultKey.of(st.getPassword()))
                    .sshIdentity(EncryptedValue.VaultKey.of(st.getSshIdentity()))
                    .perUser(false)
                    .build();
            StoreCreationDialog.showCreation(synced, DataStoreCreationCategory.IDENTITY, dataStoreEntry -> {}, true);
        }
    }
}
