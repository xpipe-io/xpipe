package io.xpipe.ext.base.identity;

import io.xpipe.app.action.AbstractAction;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.hub.action.HubLeafProvider;
import io.xpipe.app.hub.action.StoreAction;
import io.xpipe.app.hub.action.StoreActionCategory;
import io.xpipe.app.hub.comp.StoreCreationDialog;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageSyncHandler;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.LabelGraphic;

import javafx.application.Platform;
import javafx.beans.value.ObservableValue;

import javafx.scene.control.Button;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

public class LocalIdentityConvertHubLeafProvider implements HubLeafProvider<LocalIdentityStore> {

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
            StoreCreationDialog.showEdit(ref.get(), synced, _ -> {});

            // Ugly solution to sync key file if needed
            Platform.runLater(() -> {
                var found = AppMainWindow.getInstance().getStage().getScene().getRoot().lookupAll(".git-sync-file-button");
                if (found.size() != 1) {
                    return;
                }

                var first = found.iterator().next();
                if (first instanceof Button b) {
                    b.fire();
                }
            });
        }
    }
}
