package io.xpipe.ext.base.identity;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.hub.comp.StoreCreationDialog;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.LabelGraphic;

import javafx.beans.value.ObservableValue;

import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class LocalIdentityConvertActionProvider implements HubLeafProvider<LocalIdentityStore> {

    @Override
    public StoreActionCategory getCategory() {
        return StoreActionCategory.CUSTOM;
    }

    @Override
    public AbstractAction createAction(DataStoreEntryRef<LocalIdentityStore> ref) {
        return Action.builder().ref(ref).build();
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
    public LabelGraphic getIcon(DataStoreEntryRef<LocalIdentityStore> store) {
        return new LabelGraphic.IconGraphic("mdi2g-git");
    }

    @Override
    public Class<?> getApplicableClass() {
        return LocalIdentityStore.class;
    }

    @Override
    public boolean isApplicable(DataStoreEntryRef<LocalIdentityStore> o) {
        return DataStorage.get().supportsSync();
    }

    @Override
    public String getId() {
        return "convertLocalIdentity";
    }

    @Jacksonized
    @SuperBuilder
    public static class Action extends StoreAction<LocalIdentityStore> {

        @Override
        public void executeImpl() {
            var st = ref.getStore();
            var synced = SyncedIdentityStore.builder()
                    .username(st.getUsername().get())
                    .password(EncryptedValue.VaultKey.of(st.getPassword()))
                    .sshIdentity(EncryptedValue.VaultKey.of(st.getSshIdentity()))
                    .perUser(false)
                    .build();
            StoreCreationDialog.showCreation(synced, DataStoreCreationCategory.IDENTITY, dataStoreEntry -> {}, true);
        }
    }
}
