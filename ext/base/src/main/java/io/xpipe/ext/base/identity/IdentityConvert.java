package io.xpipe.ext.base.identity;

import io.xpipe.app.core.window.AppMainWindow;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.hub.comp.StoreCreationDialog;
import io.xpipe.app.secret.EncryptedValue;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import javafx.application.Platform;
import javafx.scene.control.Button;

import java.util.List;
import java.util.function.Consumer;

public class IdentityConvert {

    public static void syncLocal(
            DataStoreEntryRef<LocalIdentityStore> ref,
            boolean selectCategory,
            Consumer<DataStoreEntryRef<SyncedIdentityStore>> consumer) {
        var st = ref.getStore();
        var synced = SyncedIdentityStore.builder()
                .username(st.getUsername().get())
                .password(EncryptedValue.VaultKey.of(st.getPassword()))
                .sshIdentity(EncryptedValue.VaultKey.of(st.getSshIdentity()))
                .perUser(false)
                .build();
        StoreCreationDialog.showEdit(ref.get(), synced, true, selectCategory, updated -> {
            if (updated.getStore() instanceof SyncedIdentityStore) {
                consumer.accept(updated.ref());
            }
        });

        // Ugly solution to sync key file if needed
        Platform.runLater(() -> {
            var found = AppMainWindow.get().getStage().getScene().getRoot().lookupAll(".git-sync-file-button");
            if (found.size() != 1) {
                return;
            }

            var first = found.iterator().next();
            if (first instanceof Button b) {
                b.fire();
            }
        });
    }

    public static void createMulti(
            IdentityValue val, boolean moveToSync, Consumer<DataStoreEntryRef<MultiIdentityStore>> consumer) {
        if (!(val instanceof IdentityValue.Ref ref)) {
            throw new IllegalArgumentException("Not a identity reference");
        }

        var synced = MultiIdentityStore.builder()
                .identities(List.of(ref.getRef().get().getUuid()))
                .perUser(val.isPerUser())
                .build();
        StoreCreationDialog.showCreation(
                ref.getRef().get().getName() + "-multi",
                synced,
                DataStoreCreationCategory.IDENTITY,
                created -> {
                    if (created.getStore() instanceof MultiIdentityStore) {
                        if (moveToSync) {
                            var cat = DataStorage.get()
                                    .getStoreCategoryIfPresent(created.getCategoryUuid())
                                    .orElseThrow();
                            var inSynced = DataStorage.get().getCategoryParentHierarchy(cat).stream()
                                    .anyMatch(dataStoreCategory -> dataStoreCategory
                                            .getUuid()
                                            .equals(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID));
                            var targetCategory = DataStorage.get()
                                    .getStoreCategoryIfPresent(
                                            inSynced
                                                    ? created.getCategoryUuid()
                                                    : DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID)
                                    .orElseThrow();
                            DataStorage.get().moveEntryToCategory(created, targetCategory);
                        }
                        consumer.accept(created.ref());
                    }
                },
                false);
    }
}
