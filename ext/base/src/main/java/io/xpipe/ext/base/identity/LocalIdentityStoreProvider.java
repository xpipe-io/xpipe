package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.store.StoreEntryWrapper;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.storage.*;
import io.xpipe.app.util.EncryptedValue;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.SecretRetrievalStrategy;
import io.xpipe.app.util.SecretRetrievalStrategyHelper;
import io.xpipe.core.store.DataStore;

import javafx.beans.property.Property;
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

        var user = new SimpleStringProperty(st.getUsername());
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
                .longDescription("base:sshKey")
                .sub(SshIdentityStrategyHelper.identity(new SimpleObjectProperty<>(), identity, null, false), identity)
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
    public String summaryString(StoreEntryWrapper wrapper) {
        var st = (LocalIdentityStore) wrapper.getStore().getValue();
        return AppI18n.get("localIdentity");
    }

    @Override
    public List<String> getPossibleNames() {
        return List.of("localIdentity");
    }

    @Override
    public DataStore defaultStore() {
        return LocalIdentityStore.builder().password(EncryptedValue.of(new SecretRetrievalStrategy.None())).sshIdentity(EncryptedValue.of(new SshIdentityStrategy.None())).build();
    }

    @Override
    public List<Class<?>> getStoreClasses() {
        return List.of(LocalIdentityStore.class);
    }
}
