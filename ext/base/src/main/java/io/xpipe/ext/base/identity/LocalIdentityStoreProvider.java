package io.xpipe.ext.base.identity;

import io.xpipe.app.hub.creation.StoreCreationModel;
import io.xpipe.app.identity.NoIdentityStrategy;
import io.xpipe.app.identity.SshIdentityStrategyChoiceConfig;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.secret.*;
import io.xpipe.app.storage.*;
import io.xpipe.app.store.DataStore;
import io.xpipe.app.util.*;

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
                .anyMatch(dataStoreCategory ->
                        dataStoreCategory.getUuid().equals(DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID));
        return inLocal ? target : DataStorage.LOCAL_IDENTITIES_CATEGORY_UUID;
    }

    @Override
    public GuiDialog guiDialog(StoreCreationModel model, Property<DataStore> store) {
        LocalIdentityStore st = (LocalIdentityStore) store.getValue();

        var user = new SimpleStringProperty(st.getUsername().get());
        var pass = new SimpleObjectProperty<>(st.getPassword());
        var identity = new SimpleObjectProperty<>(st.getSshIdentity());

        var current = DataStoreAccessScope.encryption();
        var sshIdentityChoiceConfig = SshIdentityStrategyChoiceConfig.builder()
                .allowKeyFileSync(false)
                .scopeCheck(() -> current)
                .build();

        var passwordChoice = OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(pass)
                .customConfiguration(
                        SecretStrategyChoiceConfig.builder().allowNone(true).build())
                .available(SecretRetrievalStrategy.getClasses())
                .build()
                .build();

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
                .bind(
                        () -> {
                            return LocalIdentityStore.builder()
                                    .username(user.get())
                                    .password(
                                            st.getEncryptedPassword() != null
                                                    ? st.getEncryptedPassword().with(pass.get())
                                                    : OptionalEncryptedValue.of(pass.get(), current))
                                    .sshIdentity(
                                            st.getEncryptedSshIdentity() != null
                                                    ? st.getEncryptedSshIdentity()
                                                            .with(identity.get())
                                                    : OptionalEncryptedValue.of(identity.get(), current))
                                    .build();
                        },
                        store)
                .buildDialog();
    }

    @Override
    public DataStore defaultStore(DataStoreCategory category) {
        var current = DataStoreAccessScope.encryption();
        return LocalIdentityStore.builder()
                .password(OptionalEncryptedValue.of(new SecretNoneStrategy(), current))
                .sshIdentity(OptionalEncryptedValue.of(new NoIdentityStrategy(), current))
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
