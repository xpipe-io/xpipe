package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.storage.*;
import io.xpipe.app.util.*;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import java.util.List;
import java.util.UUID;

public class LocalIdentityStoreProvider extends IdentityStoreProvider {

    @Override
    public UUID getTargetCategory(DataStore store, UUID target) {
        var cat = DataStorage.get().getStoreCategoryIfPresent(target).orElseThrow();
        var inLocal = DataStorage.get().getCategoryParentHierarchy(cat).stream()
                .anyMatch(
                        dataStoreCategory -> dataStoreCategory.getUuid() == DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID);
        return inLocal ? target : DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID;
    }

    @Override
    public GuiDialog guiDialog(DataStoreEntry entry, Property<DataStore> store) {
        LocalIdentityStore st = (LocalIdentityStore) store.getValue();

        var user = new SimpleStringProperty(st.getUsername().get());
        var pass = new SimpleObjectProperty<>(st.getPassword());
        var identity = new SimpleObjectProperty<>(st.getSshIdentity());

        return new OptionsBuilder()
                .nameAndDescription("username")
                .addString(user)
                .name("passwordAuthentication")
                .description("passwordAuthenticationDescription")
                .sub(SecretRetrievalStrategyHelper.comp(pass, true), pass)
                .name("keyAuthentication")
                .description("keyAuthenticationDescription")
                .longDescription(DocumentationLink.SSH_KEYS)
                .sub(
                        SshIdentityStrategyHelper.identity(
                                new ReadOnlyObjectWrapper<>(
                                        DataStorage.get().local().ref()),
                                identity,
                                null,
                                false,
                                true),
                        identity)
                .bind(
                        () -> {
                            return LocalIdentityStore.builder()
                                    .username(user.get())
                                    .password(
                                            st.getEncryptedPassword() != null
                                                    ? st.getEncryptedPassword().withValue(pass.get())
                                                    : EncryptedValue.of(pass.get()))
                                    .sshIdentity(
                                            st.getEncryptedSshIdentity() != null
                                                    ? st.getEncryptedSshIdentity()
                                                            .withValue(identity.get())
                                                    : EncryptedValue.of(identity.get()))
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        return LocalIdentityStore.builder()
                .password(EncryptedValue.of(new SecretRetrievalStrategy.None()))
                .sshIdentity(EncryptedValue.of(new SshIdentityStrategy.None()))
                .build();
    }

    @Override
    public String getId() {
        return "localIdentity";
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(LocalIdentityStore.class);
    }
}
