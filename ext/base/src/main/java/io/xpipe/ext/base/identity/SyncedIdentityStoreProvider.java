package io.xpipe.ext.base.identity;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.hub.comp.StoreEntryWrapper;
import io.xpipe.app.storage.*;
import io.xpipe.app.util.*;

import javafx.beans.property.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class SyncedIdentityStoreProvider extends IdentityStoreProvider {

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStorage.get().supportsSync() ? DataStoreCreationCategory.IDENTITY : null;
    }

    @Override
    public UUID getTargetCategory(DataStore store, UUID target) {
        var cat = DataStorage.get().getStoreCategoryIfPresent(target).orElseThrow();
        var inSynced = DataStorage.get().getCategoryParentHierarchy(cat).stream()
                .anyMatch(dataStoreCategory ->
                        dataStoreCategory.getUuid() == DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID);
        return inSynced ? target : DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID;
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        SyncedIdentityStore st = (SyncedIdentityStore) store.getValue();

        var user = new SimpleStringProperty(st.getUsername().get());
        var pass = new SimpleObjectProperty<>(st.getPassword());
        var identity = new SimpleObjectProperty<>(st.getSshIdentity());
        var perUser = new SimpleBooleanProperty(st.isPerUser());
        perUser.addListener((observable, oldValue, newValue) -> {
            if (!(identity.getValue() instanceof SshIdentityStrategy.File f)
                    || f.getFile() == null
                    || !f.getFile().isInDataDirectory()) {
                return;
            }

            var source = Path.of(f.getFile().toAbsoluteFilePath(null).toString());
            var target = Path.of("keys", f.getFile().toAbsoluteFilePath(null).getFileName());
            DataStorageSyncHandler.getInstance().addDataFile(source, target, newValue);

            var pub = Path.of(source + ".pub");
            var pubTarget = Path.of("keys", f.getFile().toAbsoluteFilePath(null).getFileName() + ".pub");
            if (Files.exists(pub)) {
                DataStorageSyncHandler.getInstance().addDataFile(pub, pubTarget, newValue);
            }
        });

        return new OptionsBuilder()
                .nameAndDescription("username")
                .addString(user)
                .name("passwordAuthentication")
                .description("passwordAuthenticationDescription")
                .sub(SecretRetrievalStrategyHelper.comp(pass, true), pass)
                .name("keyAuthentication")
                .description("keyAuthenticationDescription")
                .longDescription("base:sshKey")
                .sub(
                        SshIdentityStrategyHelper.identity(
                                new ReadOnlyObjectWrapper<>(
                                        DataStorage.get().local().ref()),
                                identity,
                                path -> perUser.get(),
                                true,
                                true),
                        identity)
                .check(val -> Validator.create(val, AppI18n.observable("keyNotSynced"), identity, i -> {
                    var wrong = i instanceof SshIdentityStrategy.File f
                            && f.getFile() != null
                            && !f.getFile().isInDataDirectory();
                    return !wrong;
                }))
                .nameAndDescription(
                        DataStorageUserHandler.getInstance().getActiveUser() != null
                                ? "identityPerUser"
                                : "identityPerUserDisabled")
                .addToggle(perUser)
                .disable(DataStorageUserHandler.getInstance().getActiveUser() == null)
                .bind(
                        () -> {
                            return SyncedIdentityStore.builder()
                                    .username(user.get())
                                    .password(
                                            st.getEncryptedPassword() != null
                                                    ? st.getEncryptedPassword().withValue(pass.get())
                                                    : EncryptedValue.VaultKey.of(pass.get()))
                                    .sshIdentity(
                                            st.getEncryptedSshIdentity() != null
                                                    ? st.getEncryptedSshIdentity()
                                                            .withValue(identity.get())
                                                    : EncryptedValue.VaultKey.of(identity.get()))
                                    .password(EncryptedValue.VaultKey.of(pass.get()))
                                    .sshIdentity(EncryptedValue.VaultKey.of(identity.get()))
                                    .perUser(perUser.get())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        return wrapper.getEntry().isPerUserStore() ? AppI18n.get("userIdentity") : AppI18n.get("globalIdentity");
    }

    @Override
    public String getId() {
        return "syncedIdentity";
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return SyncedIdentityStore.builder()
                .password(EncryptedValue.VaultKey.of(new SecretRetrievalStrategy.None()))
                .sshIdentity(EncryptedValue.VaultKey.of(new SshIdentityStrategy.None()))
                .perUser(false)
                .build();
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(SyncedIdentityStore.class);
    }
}
