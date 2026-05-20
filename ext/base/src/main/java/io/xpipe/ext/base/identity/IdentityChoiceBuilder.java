package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.RegionBuilder;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.InputGroupComp;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.cred.SshIdentityStrategy;
import io.xpipe.app.cred.SshIdentityStrategyChoiceConfig;
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

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.beans.value.ObservableBooleanValue;
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
    ObservableBooleanValue syncedBase;
    boolean allowCustomUserInput;
    boolean requireUserInput;
    boolean requirePassword;
    boolean keyInput;
    boolean requireKeyInput;
    String userChoiceTranslationKey;
    ObservableValue<String> passwordChoiceTranslationKey;
    ObservableValue<DataStoreEntryRef<ShellStore>> fileSystem;

    public IdentityChoiceBuilder(
            ObjectProperty<IdentityValue> identity,
            ObservableBooleanValue syncedBase,
            boolean allowCustomUserInput,
            boolean requireUserInput,
            boolean requirePassword,
            boolean keyInput,
            boolean requireKeyInput,
            String userChoiceTranslationKey,
            String passwordChoiceTranslationKey) {
        this.syncedBase = syncedBase;
        this.identity = identity;
        this.allowCustomUserInput = allowCustomUserInput;
        this.requireUserInput = requireUserInput;
        this.requirePassword = requirePassword;
        this.keyInput = keyInput;
        this.requireKeyInput = requireKeyInput;
        this.userChoiceTranslationKey = userChoiceTranslationKey;
        this.passwordChoiceTranslationKey = new ReadOnlyStringWrapper(passwordChoiceTranslationKey);
        this.fileSystem = new ReadOnlyObjectWrapper<>(DataStorage.get().local().ref());

        addSyncCheckListener();
    }

    private void addSyncCheckListener() {
        identity.addListener((observable, oldValue, newValue) -> {
            if (DataStorage.get().supportsSync()
                    && syncedBase.getValue()
                    && newValue instanceof IdentityValue.Ref r
                    && r.unwrap() instanceof LocalIdentityStore) {
                var modal = ModalOverlay.of(
                        "unsyncedIdentityTitle",
                        AppDialog.dialogTextKey("unsyncedIdentityContent").prefWidth(600));
                modal.addButton(new ModalButton(
                        "documentation",
                        () -> {
                            DocumentationLink.IDENTITIES.open();
                        },
                        false,
                        false));
                modal.addButtonBarComp(RegionBuilder.hspacer());
                modal.addButton(new ModalButton(
                        "syncIdentity",
                        () -> {
                            Platform.runLater(() -> {
                                IdentityConvert.syncLocal(r.getRef().asNeeded(), false, updated -> {
                                    Platform.runLater(() -> {
                                        identity.set(null);
                                        identity.set(IdentityValue.Ref.builder().ref(updated.asNeeded()).build());
                                    });
                                });
                            });
                        },
                        true,
                        false));
                modal.addButton(new ModalButton(
                        "convertToMulti",
                        () -> {
                            IdentityConvert.createMulti(r, true, created -> {
                                Platform.runLater(() -> {
                                    identity.set(IdentityValue.Ref.builder()
                                            .ref(created.asNeeded())
                                            .build());
                                });
                            });
                        },
                        true,
                        false));
                modal.addButton(new ModalButton("ignore", null, true, false));
                modal.show();
            }
        });
    }

    public static OptionsBuilder ssh(
            ObjectProperty<IdentityValue> identity, ObservableBooleanValue syncedBase, boolean requireUser) {
        var i = new IdentityChoiceBuilder(
                identity, syncedBase, true, requireUser, true, true, true, "identityChoice", "passwordAuthentication");
        return i.build();
    }

    public static OptionsBuilder container(ObjectProperty<IdentityValue> identity, ObservableBooleanValue syncedBase) {
        var i = new IdentityChoiceBuilder(
                identity, syncedBase, true, false, false, false, false, "customUsername", "customUsernamePassword");
        return i.build();
    }

    public static OptionsBuilder keyAuthChoice(
            Property<SshIdentityStrategy> identity, SshIdentityStrategyChoiceConfig config) {
        return OptionsChoiceBuilder.builder()
                .allowNull(false)
                .property(identity)
                .customConfiguration(config)
                .available(SshIdentityStrategy.getAvailable())
                .transformer(entryComboBox -> {
                    var button = new ButtonComp(null, new LabelGraphic.IconGraphic("mdi2k-key-plus"), () -> {
                        ProcessControlProvider.get().showSshKeygenDialog(null, identity);
                    });
                    button.describe(d -> d.nameKey("generateKey"));
                    var comboComp = RegionBuilder.of(() -> entryComboBox);
                    var hbox = new InputGroupComp(List.of(comboComp, button));
                    hbox.setMainReference(comboComp);
                    return hbox.build();
                })
                .build()
                .build();
    }

    public OptionsBuilder build() {
        var user = new SimpleStringProperty();
        var pass = new SimpleObjectProperty<SecretRetrievalStrategy>();
        var identityStrategy = new SimpleObjectProperty<SshIdentityStrategy>();
        var ref = new SimpleObjectProperty<DataStoreEntryRef<IdentityStore>>();
        var inPlaceSelected = ref.isNull();
        var refSelected = ref.isNotNull();

        identity.subscribe(existing -> {
            user.set(
                    existing instanceof IdentityValue.InPlace inPlace && inPlace.unwrap() != null
                            ? inPlace.unwrap().getUsername().get()
                            : null);
            pass.set(
                    existing instanceof IdentityValue.InPlace inPlace && inPlace.unwrap() != null
                            ? inPlace.unwrap().getPassword()
                            : null);
            identityStrategy.set(
                    existing instanceof IdentityValue.InPlace inPlace && inPlace.unwrap() != null
                            ? inPlace.unwrap().getSshIdentity()
                            : null);
            ref.set(existing instanceof IdentityValue.Ref r ? r.getRef() : null);
        });

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
                .documentationLink(DocumentationLink.IDENTITIES)
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
                        }

                        return IdentityValue.InPlace.builder()
                                .identityStore(LocalIdentityStore.builder()
                                        .username(u)
                                        .password(p)
                                        .sshIdentity(i)
                                        .build())
                                .build();
                    }
                },
                identity);
        return options;
    }
}
