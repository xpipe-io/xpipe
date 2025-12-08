package io.xpipe.ext.base.identity;

import atlantafx.base.theme.Styles;
import io.xpipe.app.browser.BrowserFullSessionModel;
import io.xpipe.app.browser.file.BrowserFileOpener;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.*;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;
import io.xpipe.ext.base.identity.ssh.NoIdentityStrategy;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import lombok.Data;

import java.util.List;

public class IdentityApplyDialog {

    private static FilePath getSystemConfigPath(ShellControl sc) throws Exception {
        if (sc.getOsType() == OsType.WINDOWS) {
            var base = sc.view().getEnvironmentVariableOrThrow("programdata");
            return FilePath.of(base).join("ssh", "ssh_config");
        }

        return FilePath.of("/etc/ssh/ssh_config");
    }

    @Data
    private static class SystemState {

        public static SystemState of(ShellControl sc, IdentityStore identity) throws Exception {
            var s = new SystemState();
            s.init(sc, identity);
            return s;
        }

        boolean inAuthorizedKeys;
        boolean keyAuthEnabled;
        boolean keyAuthInMethods;
        boolean passwordAuthEnabled;
        boolean passwordAuthInMethods;
        boolean keyboardInteractiveAuthEnabled;
        boolean rootLoginEnabled;

        private void init(ShellControl sc, IdentityStore identity) throws Exception {
            var hasPassword = identity.getPassword() != null && identity.getPassword().expectsQuery();
            var hasIdentity = identity.getSshIdentity() != null && identity.getSshIdentity().getPublicKey() != null;

            var configContent = getSshdConfigContent(sc);
            keyAuthEnabled = isSet(configContent, "PubkeyAuthentication", "yes", true,false);
            passwordAuthEnabled = isSet(configContent, "PasswordAuthentication", "yes", true, false);
            keyboardInteractiveAuthEnabled = isSet(configContent, "KbdInteractiveAuthentication", "yes", false, false);
            rootLoginEnabled = isSet(configContent, "PermitRootLogin", "yes", !hasPassword, false) ||
                    (!hasPassword &&
                            !isSet(configContent, "PermitRootLogin", "forced-commands-only", true, false) &&
                            isSet(configContent, "PermitRootLogin", "prohibit-password", true, true));
            keyAuthInMethods = isSet(configContent, "AuthenticationMethods", "publickey", true, false);
            passwordAuthInMethods = isSet(configContent, "AuthenticationMethods", "password", true, false);

            if (hasIdentity) {
                var authorizedKeysContent = getAuthorizedKeysContent(sc);

                var publicKey = identity.getSshIdentity().getPublicKey();
                var split = publicKey.split("\\s+");
                var basePublicKey = split[0] + " " + split[1];

                inAuthorizedKeys = authorizedKeysContent.toLowerCase().contains(basePublicKey.toLowerCase());
            } else {
                inAuthorizedKeys = true;
            }
        }

        private static String getAuthorizedKeysContent(ShellControl sc) throws Exception {
            var v = sc.view();
            var authorizedKeysFile = v.userHome().join(".ssh", "authorized_keys");
            var authorizedKeysContent = v.fileExists(authorizedKeysFile) ? v.readTextFile(authorizedKeysFile) : "";
            return authorizedKeysContent;
        }

        private static String getSshdConfigContent(ShellControl sc) throws Exception {
            var v = sc.view();
            var configFile = getSystemConfigPath(sc);
            var configContent = v.fileExists(configFile) ? v.readTextFile(configFile) : "";
            return configContent;
        }

        private static boolean isSet(String config, String name, String value, boolean notFoundDef, boolean notSpecifiedDef) {
            var found = config.lines().filter(s -> {
                return !s.strip().startsWith("#");
            }).filter(s -> {
                return s.toLowerCase().contains(name.toLowerCase());
            }).findFirst();
            if (found.isEmpty()) {
                return notFoundDef;
            }

            var mapped = found.map(s -> s.toLowerCase().contains(value.toLowerCase()));
            return mapped.orElse(notSpecifiedDef);
        }
    }

    private static void addPublicKey(ShellControl sc, String publicKey) throws Exception {
        var v = sc.view();
        var authorizedKeysFile = v.userHome().join(".ssh", "authorized_keys");
        v.mkdir(authorizedKeysFile.getParent());
        var authorizedKeysContent = v.fileExists(authorizedKeysFile) ?
                v.readTextFile(authorizedKeysFile).strip() + "\n" + publicKey + "\n" : publicKey + "\n";
        v.writeTextFile(authorizedKeysFile, authorizedKeysContent);
        if (sc.getOsType() != OsType.WINDOWS) {
            sc.command(CommandBuilder.of().add("chmod", "600").addFile(authorizedKeysFile)).execute();
        }
    }

    private static Comp<?> success() {
        var graphic = new LabelGraphic.IconGraphic("mdi2c-checkbox-marked-outline");
        return new LabelComp(AppI18n.observable("valid"), new ReadOnlyObjectWrapper<>(graphic)).styleClass(Styles.SUCCESS).apply(struc -> {
            AppFontSizes.lg(struc.get());
        });
    }

    private static Comp<?> warning() {
        var graphic = new LabelGraphic.IconGraphic("mdi2a-alert-box-outline");
        return new LabelComp(AppI18n.observable("warning"), new ReadOnlyObjectWrapper<>(graphic)).styleClass(Styles.WARNING).apply(struc -> {
            AppFontSizes.lg(struc.get());
        });
    }

    private static Comp<?> fail(Comp<?> fixComp) {
        var graphic = new LabelGraphic.IconGraphic("mdi2c-close-box-outline");
        var label = new LabelComp(AppI18n.observable("notValid"), new ReadOnlyObjectWrapper<>(graphic)).styleClass(Styles.DANGER);
        label.apply(struc -> {
            AppFontSizes.lg(struc.get());
        });
        if (fixComp != null) {
            var hbox = new HorizontalComp(List.of(label, fixComp, Comp.hspacer()));
            hbox.spacing(10);
            return hbox;
        } else {
            return label;
        }
    }

    private static Comp<?> createAuthorizedKeysOptions(Property<DataStoreEntryRef<ShellStore>> system, ObjectProperty<SystemState> systemState, IdentityStore identity, BooleanProperty busy) {
        var showAddAuthorizedHost = BindingsHelper.mapBoolean(systemState, s -> {
            return s != null && !s.isInAuthorizedKeys();
        });

        var editButton = new ButtonComp(AppI18n.observable("identityApplyEditAuthorizedKeysButton"), () -> {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(busy, () -> {
                    var sc = system.getValue().getStore().getOrStartSession();
                    var file = sc.view().userHome().join(".ssh", "authorized_keys");
                    var model = BrowserFullSessionModel.DEFAULT.openFileSystemSync(system.getValue(), null, m -> file.getParent(), null, false);
                    var found = model.findFile(file);
                    if (found.isEmpty()) {
                        model.getFileSystem().touch(file);
                        if (sc.getOsType() != OsType.WINDOWS) {
                            sc.command(CommandBuilder.of().add("chmod", "600").addFile(file)).execute();
                        }
                        model.refreshSync();
                        found = model.findFile(file);
                    }
                    if (found.isPresent()) {
                        BrowserFileOpener.openInTextEditor(model, found.get());
                    }
                });
            });
        });

        var addButton = new ButtonComp(AppI18n.observable("identityApplyAuthorizedHostButton"),
                () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        BooleanScope.executeExclusive(busy, () -> {
                            var sc = system.getValue().getStore().getOrStartSession();
                            addPublicKey(sc, identity.getSshIdentity().getPublicKey());
                            systemState.setValue(SystemState.of(sc, identity));
                        });
                    });
                }).padding(new Insets(3, 7, 3, 7));

        var options = new OptionsBuilder()
                .addTitle("authorized_hosts")
                .nameAndDescription("identityApplyAuthorizedHost")
                .addComp(success())
                .hide(showAddAuthorizedHost)
                .nameAndDescription("identityApplyAuthorizedHost")
                .addComp(fail(addButton))
                .hide(showAddAuthorizedHost.not())
                .nameAndDescription("identityApplyEditAuthorizedKeys")
                .addComp(editButton);

        return options.buildComp().hide(Bindings.isNull(systemState));
    }

    private static Comp<?> createConfigOptions(Property<DataStoreEntryRef<ShellStore>> system, Property<SystemState> systemState, IdentityStore identity, BooleanProperty busy) {
        var showPasswordEnabledWarning = BindingsHelper.mapBoolean(systemState, s -> {
            return s != null && (identity.getPassword() == null || !identity.getPassword().expectsQuery()) && s.isPasswordAuthEnabled() && s.isPasswordAuthInMethods();
        });
        var showPasswordDisabledWarning = BindingsHelper.mapBoolean(systemState, s -> {
            return s != null && identity.getPassword() != null && identity.getPassword().expectsQuery() && (!s.isPasswordAuthEnabled() || !s.isPasswordAuthInMethods());
        });
        var showKeyEnabledWarning = BindingsHelper.mapBoolean(systemState, s -> {
            return s != null && !(identity.getSshIdentity() instanceof NoIdentityStrategy) && !s.keyAuthEnabled;
        });
        var showRootWarning = BindingsHelper.mapBoolean(systemState, s -> {
            return s != null && identity.getUsername().getFixedUsername().map(u -> u.equals("root")).orElse(false) && !s.rootLoginEnabled;
        });
        var showConfigSection = showKeyEnabledWarning.or(showRootWarning).or(showPasswordEnabledWarning).or(showPasswordDisabledWarning);

        var editButton = new ButtonComp(AppI18n.observable("identityApplyEditConfigButton"), () -> {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(busy, () -> {
                    var file = getSystemConfigPath(system.getValue().getStore().getOrStartSession());
                    var model = BrowserFullSessionModel.DEFAULT.openFileSystemSync(system.getValue(), null, m -> file.getParent(), null, false);
                    var found = model.findFile(file);
                    if (found.isEmpty()) {
                        return;
                    }
                    BrowserFileOpener.openInTextEditor(model, found.get());
                });
            });
        });

        var options = new OptionsBuilder()
                .addTitle("sshd_config")
                .nameAndDescription("identityApplyConfigPasswordEnabled")
                .addComp(warning())
                .hide(showPasswordEnabledWarning.not())
                .nameAndDescription("identityApplyConfigPasswordDisabled")
                .addComp(warning())
                .hide(showPasswordDisabledWarning.not())
                .nameAndDescription("identityApplyRootWarning")
                .addComp(Comp.empty())
                .hide(showRootWarning.not())
                .nameAndDescription("identityApplyEditConfig")
                .addComp(editButton).buildComp()
                .hide(showConfigSection.not());
        return options;
    }

    public static void show(IdentityStore identity) {
        var busy = new SimpleBooleanProperty();
        var system = new SimpleObjectProperty<DataStoreEntryRef<ShellStore>>();
        var systemState = new SimpleObjectProperty<SystemState>();
        system.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(busy, () -> {
                    var sc = newValue.getStore().getOrStartSession();
                    systemState.setValue(SystemState.of(sc, identity));
                });
            });
        });

        var systemChoice = new StoreChoiceComp<>(null, system, ShellStore.class, null, StoreViewState.get().getAllConnectionsCategory());
        var systemChoiceBusy = new LoadingOverlayComp(systemChoice, busy, false);

        var options = new OptionsBuilder()
                .nameAndDescription("identityApplyTargetHost")
                .addComp(systemChoiceBusy, system)
                .addComp(createAuthorizedKeysOptions(system, systemState, identity, busy))
                .addComp(createConfigOptions(system, systemState, identity, busy));

        var modal = ModalOverlay.of("identityApplyTitle", options.buildComp().prefWidth(600).prefHeight(500));
        modal.addButton(ModalButton.cancel());
        modal.addButton(ModalButton.ok(() -> {

        }).augment(button -> {
            button.disableProperty().bind(PlatformThread.sync(busy));
        }));
        modal.show();
    }
}
