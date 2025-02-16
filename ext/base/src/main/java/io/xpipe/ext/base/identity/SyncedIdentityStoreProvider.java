package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.DataStoreCreationCategory;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.storage.*;
import io.xpipe.app.util.*;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileNames;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class SyncedIdentityStoreProvider extends IdentityStoreProvider {

    @Override
    public DataStoreCreationCategory getCreationCategory() {
        return DataStorage.get().supportsSharing() ? DataStoreCreationCategory.IDENTITY : null;
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

        var user = new SimpleStringProperty(st.getUsername());
        var pass = new SimpleObjectProperty<>(st.getPassword());
        var identity = new SimpleObjectProperty<>(st.getSshIdentity());
        var perUser = new SimpleBooleanProperty(st.isPerUser());
        perUser.addListener((observable, oldValue, newValue) -> {
            if (!(identity.getValue() instanceof SshIdentityStrategy.File f)
                    || f.getFile() == null
                    || !f.getFile().isInDataDirectory()) {
                return;
            }

            var source = Path.of(f.getFile().toAbsoluteFilePath(null));
            var target = Path.of("keys", FileNames.getFileName(f.getFile().toAbsoluteFilePath(null)));
            DataStorageSyncHandler.getInstance().addDataFile(source, target, newValue);
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
                                new SimpleObjectProperty<>(), identity, path -> perUser.get(), true, true),
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
    public DataStore defaultStore() {
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
