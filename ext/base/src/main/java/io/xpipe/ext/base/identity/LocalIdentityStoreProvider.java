package io.xpipe.ext.base.identity;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.GuiDialog;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.secret.EncryptedValue;
import io.xpipe.app.secret.SecretNoneStrategy;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.secret.SecretStrategyChoiceConfig;
import io.xpipe.app.storage.*;
import io.xpipe.app.util.*;
import io.xpipe.ext.base.identity.ssh.NoneStrategy;
import io.xpipe.ext.base.identity.ssh.SshIdentityStrategy;
import io.xpipe.ext.base.identity.ssh.SshIdentityStrategyChoiceConfig;

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

        var sshIdentityChoiceConfig = SshIdentityStrategyChoiceConfig.builder()
                .allowAgentForward(true)
                .proxy(new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref()))
                .allowKeyFileSync(false)
                .perUserKeyFileCheck(() -> false)
                .build();

        var passwordChoice = OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(pass)
                .customConfiguration(
                        SecretStrategyChoiceConfig.builder().allowNone(true).build())
                .available(SecretRetrievalStrategy.getSubclasses())
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
                .sub(
                        OptionsChoiceBuilder.builder()
                                .allowNull(false)
                                .property(identity)
                                .customConfiguration(sshIdentityChoiceConfig)
                                .available(SshIdentityStrategy.getSubclasses())
                                .build()
                                .build(),
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
                .password(EncryptedValue.of(new SecretNoneStrategy()))
                .sshIdentity(EncryptedValue.of(new NoneStrategy()))
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
