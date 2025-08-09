package io.xpipe.ext.base.identity;

import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.comp.base.ContextualFileReferenceSync;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.storage.ContextualFileReference;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.*;

import javafx.beans.property.*;
import javafx.beans.value.ObservableValue;

import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.function.Predicate;

public class SshIdentityStrategyHelper {

    private static OptionsBuilder agent(Property<SshIdentityStrategy.SshAgent> p, boolean allowForward) {
        var forward =
                new SimpleBooleanProperty(p.getValue() != null && p.getValue().isForwardAgent());
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder()
                .nameAndDescription("forwardAgent")
                .addToggle(forward)
                .nonNull()
                .hide(!allowForward)
                .nameAndDescription("publicKey")
                .addComp(
                        new TextFieldComp(publicKey).apply(struc -> struc.get()
                                .setPromptText("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIBmhLUTJiP...== Your Comment")),
                        publicKey)
                .bind(
                        () -> {
                            return new SshIdentityStrategy.SshAgent(forward.get(), publicKey.get());
                        },
                        p);
    }

    private static OptionsBuilder gpgAgent(Property<SshIdentityStrategy.GpgAgent> p, boolean allowForward) {
        var forward =
                new SimpleBooleanProperty(p.getValue() != null && p.getValue().isForwardAgent());
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder()
                .nameAndDescription("forwardAgent")
                .addToggle(forward)
                .nonNull()
                .hide(!allowForward)
                .nameAndDescription("publicKey")
                .addComp(
                        new TextFieldComp(publicKey).apply(struc -> struc.get()
                                .setPromptText("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIBmhLUTJiP...== Your Comment")),
                        publicKey)
                .bind(
                        () -> {
                            return new SshIdentityStrategy.GpgAgent(forward.get(), publicKey.get());
                        },
                        p);
    }

    private static OptionsBuilder pageant(Property<SshIdentityStrategy.Pageant> p, boolean allowForward) {
        var forward =
                new SimpleBooleanProperty(p.getValue() != null && p.getValue().isForwardAgent());
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder()
                .nameAndDescription("forwardAgent")
                .addToggle(forward)
                .nonNull()
                .hide(!allowForward)
                .nameAndDescription("publicKey")
                .addComp(
                        new TextFieldComp(publicKey).apply(struc -> struc.get()
                                .setPromptText("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIBmhLUTJiP...== Your Comment")),
                        publicKey)
                .bind(
                        () -> {
                            return new SshIdentityStrategy.Pageant(forward.get(), publicKey.get());
                        },
                        p);
    }

    private static OptionsBuilder passwordManagerAgent(
            Property<SshIdentityStrategy.PasswordManagerAgent> p, boolean allowForward) {
        var forward =
                new SimpleBooleanProperty(p.getValue() != null && p.getValue().isForwardAgent());
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder()
                .nameAndDescription("forwardAgent")
                .addToggle(forward)
                .nonNull()
                .hide(!allowForward)
                .nameAndDescription("publicKey")
                .addComp(
                        new TextFieldComp(publicKey).apply(struc -> struc.get()
                                .setPromptText("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIBmhLUTJiP...== Your Comment")),
                        publicKey)
                .bind(
                        () -> {
                            return new SshIdentityStrategy.PasswordManagerAgent(forward.get(), publicKey.get());
                        },
                        p);
    }

    private static OptionsBuilder otherExternal(Property<SshIdentityStrategy.OtherExternal> p, boolean allowForward) {
        var forward =
                new SimpleBooleanProperty(p.getValue() != null && p.getValue().isForwardAgent());
        var publicKey =
                new SimpleStringProperty(p.getValue() != null ? p.getValue().getPublicKey() : null);
        return new OptionsBuilder()
                .nameAndDescription("forwardAgent")
                .addToggle(forward)
                .nonNull()
                .hide(!allowForward)
                .nameAndDescription("publicKey")
                .addComp(
                        new TextFieldComp(publicKey).apply(struc -> struc.get()
                                .setPromptText("ssh-rsa AAAAB3NzaC1yc2EAAAABJQAAAIBmhLUTJiP...== Your Comment")),
                        publicKey)
                .bind(
                        () -> {
                            return new SshIdentityStrategy.OtherExternal(forward.get(), publicKey.get());
                        },
                        p);
    }

    private static OptionsBuilder customPkcs11Library(Property<SshIdentityStrategy.CustomPkcs11Library> p) {
        var file =
                new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getFile() : null);
        return new OptionsBuilder()
                .nameAndDescription("pkcs11Library")
                .addComp(
                        new ContextualFileReferenceChoiceComp(
                                new ReadOnlyObjectWrapper<>(
                                        DataStorage.get().local().ref()),
                                file,
                                null,
                                List.of()),
                        file)
                .nonNull()
                .bind(
                        () -> {
                            return new SshIdentityStrategy.CustomPkcs11Library(file.get());
                        },
                        p);
    }

    private static OptionsBuilder fileIdentity(
            Property<DataStoreEntryRef<ShellStore>> proxy,
            Property<SshIdentityStrategy.File> fileProperty,
            Predicate<Path> perUserFile,
            boolean allowSync) {
        var keyPath = new SimpleObjectProperty<>(
                fileProperty.getValue() != null && fileProperty.getValue().getFile() != null
                        ? fileProperty.getValue().getFile().toAbsoluteFilePath(null)
                        : null);
        fileProperty.addListener((observable, oldValue, newValue) -> {
            if (keyPath.get() != null
                    && newValue != null
                    && !ContextualFileReference.of(keyPath.get()).equals(newValue.getFile())) {
                return;
            }

            keyPath.setValue(
                    newValue != null && newValue.getFile() != null
                            ? newValue.getFile().toAbsoluteFilePath(null)
                            : null);
        });
        var keyPasswordProperty = new SimpleObjectProperty<>(
                fileProperty.getValue() != null ? fileProperty.getValue().getPassword() : null);

        var sync = new ContextualFileReferenceSync(
                Path.of("keys"), perUserFile, path -> Path.of("keys").resolve(path.getFileName()));

        return new OptionsBuilder()
                .name("location")
                .description("locationDescription")
                .addComp(
                        new ContextualFileReferenceChoiceComp(proxy, keyPath, allowSync ? sync : null, List.of()),
                        keyPath)
                .nonNull()
                .name("keyPassword")
                .description("sshConfigHost.identityPassphraseDescription")
                .sub(SecretRetrievalStrategyHelper.comp(keyPasswordProperty, true), keyPasswordProperty)
                .nonNull()
                .bind(
                        () -> {
                            return new SshIdentityStrategy.File(
                                    ContextualFileReference.of(keyPath.get()), keyPasswordProperty.get());
                        },
                        fileProperty);
    }

    public static OptionsBuilder identity(
            Property<DataStoreEntryRef<ShellStore>> proxy,
            Property<SshIdentityStrategy> strategyProperty,
            Predicate<Path> perUserFile,
            boolean allowSync,
            boolean allowForward) {
        SshIdentityStrategy strat = strategyProperty.getValue();
        var file = new SimpleObjectProperty<>(
                strat instanceof SshIdentityStrategy.File f
                        ? f
                        : new SshIdentityStrategy.File(null, new SecretRetrievalStrategy.None()));
        var customPkcs11 =
                new SimpleObjectProperty<>(strat instanceof SshIdentityStrategy.CustomPkcs11Library f ? f : null);
        var agent = new SimpleObjectProperty<>(strat instanceof SshIdentityStrategy.SshAgent a ? a : null);
        var pageant = new SimpleObjectProperty<>(strat instanceof SshIdentityStrategy.Pageant a ? a : null);
        var passwordManagerAgent =
                new SimpleObjectProperty<>(strat instanceof SshIdentityStrategy.PasswordManagerAgent a ? a : null);
        var gpgAgent = new SimpleObjectProperty<>(strat instanceof SshIdentityStrategy.GpgAgent a ? a : null);
        var otherExternal = new SimpleObjectProperty<>(strat instanceof SshIdentityStrategy.OtherExternal a ? a : null);

        var gpgFeature = LicenseProvider.get().getFeature("gpgAgent");
        var pkcs11Feature = LicenseProvider.get().getFeature("pkcs11Identity");

        var map = new LinkedHashMap<ObservableValue<String>, OptionsBuilder>();
        map.put(AppI18n.observable("base.none"), new OptionsBuilder());
        map.put(AppI18n.observable("base.keyFile"), fileIdentity(proxy, file, perUserFile, allowSync));
        map.put(AppI18n.observable("base.sshAgent"), agent(agent, allowForward));
        map.put(AppI18n.observable("passwordManagerAgent"), passwordManagerAgent(passwordManagerAgent, allowForward));
        map.put(AppI18n.observable("base.pageant"), pageant(pageant, allowForward));
        map.put(gpgFeature.suffixObservable("base.gpgAgent"), gpgAgent(gpgAgent, allowForward));
        map.put(pkcs11Feature.suffixObservable("base.yubikeyPiv"), new OptionsBuilder());
        map.put(pkcs11Feature.suffixObservable("base.customPkcs11Library"), customPkcs11Library(customPkcs11));
        map.put(AppI18n.observable("base.otherExternal"), otherExternal(otherExternal, allowForward));
        var identityMethodSelected = new SimpleIntegerProperty(
                strat instanceof SshIdentityStrategy.None
                        ? 0
                        : strat instanceof SshIdentityStrategy.File
                                ? 1
                                : strat instanceof SshIdentityStrategy.SshAgent
                                        ? 2
                                        : strat instanceof SshIdentityStrategy.PasswordManagerAgent
                                                ? 3
                                                : strat instanceof SshIdentityStrategy.Pageant
                                                        ? 4
                                                        : strat instanceof SshIdentityStrategy.GpgAgent
                                                                ? 5
                                                                : strat instanceof SshIdentityStrategy.YubikeyPiv
                                                                        ? 6
                                                                        : strat
                                                                                        instanceof
                                                                                        SshIdentityStrategy
                                                                                                .CustomPkcs11Library
                                                                                ? 7
                                                                                : strat
                                                                                                instanceof
                                                                                                SshIdentityStrategy
                                                                                                        .OtherExternal
                                                                                        ? 8
                                                                                        : strat == null ? -1 : 0);
        return new OptionsBuilder()
                .longDescription(DocumentationLink.SSH_KEYS)
                .choice(identityMethodSelected, map)
                .bindChoice(
                        () -> {
                            return switch (identityMethodSelected.get()) {
                                case 0 -> new SimpleObjectProperty<>(new SshIdentityStrategy.None());
                                case 1 -> file;
                                case 2 -> agent;
                                case 3 -> passwordManagerAgent;
                                case 4 -> pageant;
                                case 5 -> gpgAgent;
                                case 6 -> new SimpleObjectProperty<>(new SshIdentityStrategy.YubikeyPiv());
                                case 7 -> customPkcs11;
                                case 8 -> otherExternal;
                                default -> new SimpleObjectProperty<>();
                            };
                        },
                        strategyProperty);
    }
}
