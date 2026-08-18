package io.xpipe.ext.base.identity;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.hub.creation.StoreCreationModel;
import io.xpipe.app.hub.entry.StoreEntryWrapper;
import io.xpipe.app.identity.KeyFileStrategy;
import io.xpipe.app.identity.NoIdentityStrategy;
import io.xpipe.app.identity.SshIdentityStrategy;
import io.xpipe.app.identity.SshIdentityStrategyChoiceConfig;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.platform.Validator;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.DataStorageAccessType;
import io.xpipe.app.secret.*;
import io.xpipe.app.storage.*;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.util.*;

import javafx.beans.property.*;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.UUID;

public class SyncedIdentityStoreProvider extends IdentityStoreProvider {

    @Override
    public boolean allowCreation() {
        return AppPrefs.get().enableGitStorage().get();
    }

    @Override
    public UUID getTargetCategory(DataStore store, UUID target) {
        var cat = DataStorage.get().getStoreCategoryIfPresent(target).orElseThrow();
        var inSynced = DataStorage.get().getCategoryParentHierarchy(cat).stream()
                .anyMatch(dataStoreCategory ->
                        dataStoreCategory.getUuid().equals(DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID));
        return inSynced ? target : DataStorage.SYNCED_IDENTITIES_CATEGORY_UUID;
    }

    @Override
    public GuiDialog guiDialog(StoreCreationModel model, Property<DataStore> store) {
        SyncedIdentityStore st = (SyncedIdentityStore) store.getValue();

        var user = new SimpleStringProperty(st.getUsername().get());
        var pass = new SimpleObjectProperty<>(st.getPassword());
        var identity = new SimpleObjectProperty<>(st.getSshIdentity());
        var scope = new SimpleObjectProperty<>(st.getAccessScope());
        scope.addListener((observable, oldValue, newValue) -> {
            if (!(identity.getValue() instanceof KeyFileStrategy f)
                    || f.getFile() == null
                    || !f.getFile().isInDataDirectory()) {
                return;
            }

            var source = Path.of(f.getFile().toAbsoluteFilePath(null).toString());
            var target = DataStorage.get()
                    .getDataDir()
                    .resolve("keys", f.getFile().toAbsoluteFilePath(null).getFileName());
            DataStorageSyncHandler.getInstance().addDataFile(source, target, newValue);

            var pub = SshIdentityStrategy.getPublicKeyPath(FilePath.of(source)).asLocalPath();
            var pubTarget = DataStorage.get()
                    .getDataDir()
                    .resolve("keys", pub.getFileName().toString());
            if (Files.exists(pub)) {
                DataStorageSyncHandler.getInstance().addDataFile(pub, pubTarget, newValue);
            }
        });

        var sshIdentityChoiceConfig = SshIdentityStrategyChoiceConfig.builder()
                .allowKeyFileSync(true)
                .scopeCheck(() -> scope.get())
                .build();

        var passwordChoice = OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(pass)
                .customConfiguration(
                        SecretStrategyChoiceConfig.builder().allowNone(true).build())
                .available(SecretRetrievalStrategy.getClasses())
                .build()
                .build();

        var roleBased = DataStorageAccessHandler.getInstance().isAccessRestricted()
                && DataStorageAccessHandler.getInstance().getType() == DataStorageAccessType.ROLE;
        return new OptionsBuilder()
                .nameAndDescription("username")
                .addString(user)
                .name("passwordAuthentication")
                .description("passwordAuthenticationDescription")
                .sub(passwordChoice, pass)
                .name("keyAuthentication")
                .description("keyAuthenticationDescription")
                .documentationLink(DocumentationLink.SSH_KEYS)
                .sub(IdentityChoiceBuilder.keyAuthChoice(identity, sshIdentityChoiceConfig), identity)
                .check(val -> Validator.create(val, AppI18n.observable("keyNotSynced"), identity, i -> {
                    var wrong = i instanceof KeyFileStrategy f
                            && f.getFile() != null
                            && !f.getFile().isInDataDirectory();
                    return !wrong;
                }))
                .nameAndDescription(roleBased ? "identityPerRole" : "identityPerRoleDisabled")
                .addComp(new DataStoreAccessScopeComp(scope), scope)
                .nonNull()
                .bind(
                        () -> {
                            return SyncedIdentityStore.builder()
                                    .username(user.get())
                                    .password(
                                            st.getEncryptedPassword() != null
                                                    ? st.getEncryptedPassword().with(pass.get(), scope.get())
                                                    : EncryptedValue.of(pass.get(), scope.get()))
                                    .sshIdentity(
                                            st.getEncryptedSshIdentity() != null
                                                    ? st.getEncryptedSshIdentity()
                                                            .with(identity.get(), scope.get())
                                                    : EncryptedValue.of(identity.get(), scope.get()))
                                    .password(EncryptedValue.of(pass.get(), scope.get()))
                                    .sshIdentity(EncryptedValue.of(identity.get(), scope.get()))
                                    .accessScope(scope.get())
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public String summaryString(StoreEntryWrapper wrapper) {
        if (!wrapper.getEntry().getAccessScope().isAccessRestricted()) {
            return AppI18n.get("globalIdentity");
        }

        return (DataStorageAccessHandler.getInstance().getType() == DataStorageAccessType.ROLE
                ? AppI18n.get("roleIdentity")
                : AppI18n.get("userIdentity"));
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return SyncedIdentityStore.builder()
                .password(EncryptedValue.of(new SecretNoneStrategy(), DataStoreAccessScope.encryption()))
                .sshIdentity(EncryptedValue.of(new NoIdentityStrategy(), DataStoreAccessScope.encryption()))
                .accessScope(DataStoreAccessScope.encryption())
                .build();
    }

    @Override
    public String getId() {
        return "syncedIdentity";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(SyncedIdentityStore.class);
    }
}
