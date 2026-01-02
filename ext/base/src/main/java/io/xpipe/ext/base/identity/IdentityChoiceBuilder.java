package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.FileSystemStore;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.OptionsChoiceBuilder;
import io.xpipe.app.secret.EncryptedValue;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.secret.SecretStrategyChoiceConfig;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;
import io.xpipe.ext.base.identity.ssh.SshIdentityStrategy;
import io.xpipe.ext.base.identity.ssh.SshIdentityStrategyChoiceConfig;

import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;

import java.util.List;

@Value
@Builder
@AllArgsConstructor
public class IdentityChoiceBuilder {

    ObjectProperty<IdentityValue> identity;
    boolean allowCustomUserInput;
    boolean requireUserInput;
    boolean requirePassword;
    boolean keyInput;
    boolean requireKeyInput;
    boolean allowAgentForward;
    String userChoiceTranslationKey;
    ObservableValue<String> passwordChoiceTranslationKey;
    ObservableValue<DataStoreEntryRef<ShellStore>> fileSystem;

    public IdentityChoiceBuilder(
            ObjectProperty<IdentityValue> identity,
            boolean allowCustomUserInput,
            boolean requireUserInput,
            boolean requirePassword,
            boolean keyInput,
            boolean requireKeyInput,
            boolean allowAgentForward,
            String userChoiceTranslationKey,
            String passwordChoiceTranslationKey) {
        this.identity = identity;
        this.allowCustomUserInput = allowCustomUserInput;
        this.requireUserInput = requireUserInput;
        this.requirePassword = requirePassword;
        this.keyInput = keyInput;
        this.requireKeyInput = requireKeyInput;
        this.allowAgentForward = allowAgentForward;
        this.userChoiceTranslationKey = userChoiceTranslationKey;
        this.passwordChoiceTranslationKey = new ReadOnlyStringWrapper(passwordChoiceTranslationKey);
        this.fileSystem = new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref());
    }

    public static OptionsBuilder ssh(ObjectProperty<IdentityValue> identity, boolean requireUser) {
        var i = new IdentityChoiceBuilder(
                identity, true, requireUser, true, true, true, true, "identityChoice", "passwordAuthentication");
        return i.build();
    }

    public static OptionsBuilder container(ObjectProperty<IdentityValue> identity) {
        var i = new IdentityChoiceBuilder(
                identity, true, false, false, false, false, false, "customUsername", "customUsernamePassword");
        return i.build();
    }

    public static OptionsBuilder keyAuthChoice(
            Property<SshIdentityStrategy> identity, SshIdentityStrategyChoiceConfig config) {
        return OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(identity)
                .customConfiguration(config)
                .available(SshIdentityStrategy.getSubclasses())
                .transformer(entryComboBox -> {
                    var button = new ButtonComp(null, new LabelGraphic.IconGraphic("mdi2k-key-plus"), () -> {
                        ProcessControlProvider.get().showSshKeygenDialog(null, identity);
                    });
                    button.descriptor(d -> d.nameKey("generateKey"));
                    var comboComp = Comp.of(() -> entryComboBox);
                    var hbox = new InputGroupComp(List.of(comboComp, button));
                    hbox.setMainReference(comboComp);
                    return hbox.createRegion();
                })
                .build()
                .build();
    }

    public OptionsBuilder build() {
        var existing = identity.getValue();
        var user = new SimpleStringProperty(
                existing instanceof IdentityValue.InPlace inPlace && inPlace.unwrap() != null
                        ? inPlace.unwrap().getUsername().get()
                        : null);
        var pass = new SimpleObjectProperty<>(
                existing instanceof IdentityValue.InPlace inPlace && inPlace.unwrap() != null
                        ? inPlace.unwrap().getPassword()
                        : null);
        var identityStrategy = new SimpleObjectProperty<>(
                existing instanceof IdentityValue.InPlace inPlace && inPlace.unwrap() != null
                        ? inPlace.unwrap().getSshIdentity()
                        : null);
        var ref = new SimpleObjectProperty<>(existing instanceof IdentityValue.Ref r ? r.getRef() : null);
        var inPlaceSelected = ref.isNull();
        var refSelected = ref.isNotNull();

        var passwordChoice = OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(pass)
                .customConfiguration(
                        SecretStrategyChoiceConfig.builder().allowNone(true).build())
                .available(SecretRetrievalStrategy.getClasses())
                .build()
                .build();

        var options = new OptionsBuilder()
                .nameAndDescription(userChoiceTranslationKey)
                .addComp(new IdentitySelectComp(ref, user, pass, identityStrategy, allowCustomUserInput), user)
                .nonNullIf(inPlaceSelected.and(new SimpleBooleanProperty(requireUserInput)))
                .name(Bindings.createStringBinding(
                        () -> {
                            return AppI18n.get(passwordChoiceTranslationKey.getValue());
                        },
                        passwordChoiceTranslationKey,
                        AppI18n.activeLanguage()))
                .description(Bindings.createStringBinding(
                        () -> {
                            return AppI18n.get(passwordChoiceTranslationKey.getValue() + "Description");
                        },
                        passwordChoiceTranslationKey,
                        AppI18n.activeLanguage()))
                .sub(passwordChoice, pass)
                .nonNullIf(inPlaceSelected.and(new SimpleBooleanProperty(requirePassword)))
                .hide(refSelected)
                .addProperty(ref);

        var sshIdentityChoiceConfig = SshIdentityStrategyChoiceConfig.builder()
                .allowAgentForward(allowAgentForward)
                .allowKeyFileSync(true)
                .perUserKeyFileCheck(() -> false)
                .fileSystem(fileSystem)
                .build();

        if (keyInput) {
            options.name("keyAuthentication")
                    .description("keyAuthenticationDescription")
                    .documentationLink(DocumentationLink.SSH_KEYS)
                    .sub(keyAuthChoice(identityStrategy, sshIdentityChoiceConfig), identityStrategy)
                    .nonNullIf(inPlaceSelected.and(new ReadOnlyBooleanWrapper(requireKeyInput)))
                    .hide(refSelected);
        }
        options.bind(
                () -> {
                    if (ref.get() != null) {
                        return IdentityValue.Ref.builder().ref(ref.get()).build();
                    } else {
                        var u = user.get();
                        // In case of team vaults, identities shouldn't really be specified inline anyway
                        // If they are, we use the vault key to make it accessible for all users
                        var useUserKey = DataStorageUserHandler.getInstance().getUserCount() <= 1;
                        var p = useUserKey
                                ? EncryptedValue.CurrentKey.of(pass.get())
                                : EncryptedValue.VaultKey.of(pass.get());
                        EncryptedValue<SshIdentityStrategy> i = keyInput
                                ? (useUserKey
                                        ? EncryptedValue.CurrentKey.of(identityStrategy.get())
                                        : EncryptedValue.VaultKey.of(identityStrategy.get()))
                                : null;
                        if (u == null && p == null && i == null) {
                            return null;
                        } else {
                            return IdentityValue.InPlace.builder()
                                    .identityStore(LocalIdentityStore.builder()
                                            .username(u)
                                            .password(p)
                                            .sshIdentity(i)
                                            .build())
                                    .build();
                        }
                    }
                },
                identity);
        return options;
    }
}
