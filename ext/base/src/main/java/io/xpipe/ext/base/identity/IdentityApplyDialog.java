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
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.BindingsHelper;
import io.xpipe.app.platform.LabelGraphic;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.platform.PlatformThread;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;
import javafx.beans.binding.Bindings;
import javafx.beans.property.*;
import javafx.geometry.Insets;
import lombok.Data;

import java.util.List;

public class IdentityApplyDialog {


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
        boolean rootLoginEnabled;
        boolean mightRequireAdministratorAuthorizedKeys;

        FilePath configFile;
        FilePath authorizedKeysFile;

        private void init(ShellControl sc, IdentityStore identity) throws Exception {
            var hasPassword = identity.getPassword() != null && identity.getPassword().expectsQuery();
            var hasIdentity = identity.getSshIdentity() != null && identity.getSshIdentity().getPublicKey() != null;

            configFile = getSystemConfigPath(sc);
            authorizedKeysFile = getAuthorizedKeysFile(sc);

            var configContent = getSshdConfigContent(sc);
            keyAuthEnabled = isSet(configContent, "PubkeyAuthentication", "yes", true,false);
            passwordAuthEnabled = isSet(configContent, "PasswordAuthentication", "yes", true, false);
            rootLoginEnabled = isSet(configContent, "PermitRootLogin", "yes", !hasPassword, false) ||
                    (!hasPassword &&
                            !isSet(configContent, "PermitRootLogin", "forced-commands-only", true, false) &&
                            isSet(configContent, "PermitRootLogin", "prohibit-password", true, true));
            keyAuthInMethods = isSet(configContent, "AuthenticationMethods", "publickey", true, false);
            passwordAuthInMethods = isSet(configContent, "AuthenticationMethods", "password", true, false);
            mightRequireAdministratorAuthorizedKeys = sc.getOsType() == OsType.WINDOWS &&
                    isSet(configContent, "Match", "Group administrators", false, false) &&
                    isSet(configContent, "AuthorizedKeysFile", "administrators_authorized_keys", false, false);

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

        private FilePath getSystemConfigPath(ShellControl sc) throws Exception {
            if (sc.getOsType() == OsType.WINDOWS) {
                var base = sc.view().getEnvironmentVariableOrThrow("programdata");
                return FilePath.of(base).join("ssh", "sshd_config");
            }

            return FilePath.of("/etc/ssh/sshd_config");
        }

        private FilePath getAuthorizedKeysFile(ShellControl sc) throws Exception {
            var v = sc.view();
            var authorizedKeysFile = v.userHome().join(".ssh", "authorized_keys");
            return authorizedKeysFile;
        }

        private String getAuthorizedKeysContent(ShellControl sc) throws Exception {
            var v = sc.view();
            var authorizedKeysContent = v.fileExists(authorizedKeysFile) ? v.readTextFile(authorizedKeysFile) : "";
            return authorizedKeysContent;
        }

        private String getSshdConfigContent(ShellControl sc) throws Exception {
            var v = sc.view();
            var configContent = v.fileExists(configFile) ? v.readTextFile(configFile) : "";
            return configContent;
        }

        private boolean isSet(String config, String name, String value, boolean notFoundDef, boolean notSpecifiedDef) {
            var found = config.lines().filter(s -> {
                return !s.strip().startsWith("#");
            }).filter(s -> {
                return s.toLowerCase().contains(name.toLowerCase());
            }).toList();
            if (found.isEmpty()) {
                return notFoundDef;
            }

            for (String line : found) {
                var matches = line.toLowerCase().contains(value.toLowerCase());
                if (matches) {
                    return true;
                }
            }

            return notSpecifiedDef;
        }
    }

    private static void addPublicKey(SystemState systemState, ShellControl sc, String publicKey) throws Exception {
        var v = sc.view();
        var authorizedKeysFile = systemState.getAuthorizedKeysFile();
        v.mkdir(authorizedKeysFile.getParent());
        String authorizedKeysContent;
        if (v.fileExists(authorizedKeysFile)) {
            var text = v.readTextFile(authorizedKeysFile).strip();
            authorizedKeysContent = text.isBlank() ? publicKey + "\n" : text + "\n" + publicKey + "\n";
        } else {
            authorizedKeysContent = publicKey + "\n";
        }
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
                    var file = systemState.get().getAuthorizedKeysFile();
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
        }).padding(new Insets(4, 8, 4, 8));

        var addButton = new ButtonComp(AppI18n.observable("identityApplyAuthorizedHostButton"),
                () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        BooleanScope.executeExclusive(busy, () -> {
                            var sc = system.getValue().getStore().getOrStartSession();
                            addPublicKey(systemState.get(), sc, identity.getSshIdentity().getPublicKey());
                            systemState.setValue(SystemState.of(sc, identity));
                        });
                    });
                }).padding(new Insets(4, 8, 4, 8));

        var options = new OptionsBuilder()
                .addTitle(new ReadOnlyStringWrapper("authorized_hosts"))
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
        var showAdminWarning = BindingsHelper.mapBoolean(systemState, s -> {
            return s != null && identity.getSshIdentity() != null && identity.getSshIdentity().providesKey() && s.isMightRequireAdministratorAuthorizedKeys();
        });
        var showPasswordEnabledWarning = BindingsHelper.mapBoolean(systemState, s -> {
            return s != null && (identity.getPassword() == null || !identity.getPassword().expectsQuery()) && s.isPasswordAuthEnabled() && s.isPasswordAuthInMethods();
        });
        var showPasswordDisabledWarning = BindingsHelper.mapBoolean(systemState, s -> {
            return s != null && identity.getPassword() != null && identity.getPassword().expectsQuery() && (!s.isPasswordAuthEnabled() || !s.isPasswordAuthInMethods());
        });
        var showKeyEnabledWarning = BindingsHelper.mapBoolean(systemState, s -> {
            return s != null && (identity.getSshIdentity() == null || !identity.getSshIdentity().providesKey()) && s.keyAuthEnabled;
        });
        var showKeyDisabledWarning = BindingsHelper.mapBoolean(systemState, s -> {
            return s != null && identity.getSshIdentity() != null && identity.getSshIdentity().providesKey() && !s.keyAuthEnabled;
        });
        var showRootDisabledWarning = BindingsHelper.mapBoolean(systemState, s -> {
            return s != null && identity.getUsername().getFixedUsername().map(u -> u.equals("root")).orElse(false) && !s.rootLoginEnabled;
        });
        var showConfigSection = showKeyEnabledWarning.or(showRootDisabledWarning).or(showPasswordEnabledWarning)
                .or(showPasswordDisabledWarning).or(showKeyDisabledWarning).or(showAdminWarning);

        var editButton = new ButtonComp(AppI18n.observable("identityApplyEditConfigButton"), () -> {
            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(busy, () -> {
                    var file = systemState.getValue().getConfigFile();
                    var model = BrowserFullSessionModel.DEFAULT.openFileSystemSync(system.getValue(), null, m -> file.getParent(), null, false);
                    var found = model.findFile(file);
                    if (found.isEmpty()) {
                        return;
                    }
                    BrowserFileOpener.openInTextEditor(model, found.get());
                });
            });
        }).padding(new Insets(4, 8, 4, 8));

        var options = new OptionsBuilder()
                .addTitle(new ReadOnlyStringWrapper("sshd_config"))
                .nameAndDescription("identityApplyConfigPasswordEnabled")
                .addComp(warning())
                .hide(showPasswordEnabledWarning.not())
                .nameAndDescription("identityApplyConfigPasswordDisabled")
                .addComp(warning())
                .hide(showPasswordDisabledWarning.not())
                .nameAndDescription("identityApplyConfigKeyEnabled")
                .addComp(warning())
                .hide(showKeyEnabledWarning.not())
                .nameAndDescription("identityApplyConfigKeyDisabled")
                .addComp(warning())
                .hide(showKeyDisabledWarning.not())
                .nameAndDescription("identityApplyConfigRootDisabledWarning")
                .addComp(fail(null))
                .hide(showRootDisabledWarning.not())
                .nameAndDescription("identityApplyConfigAdminWarning")
                .documentationLink("https://learn.microsoft.com/en-us/windows-server/administration/openssh/openssh_keymanagement#administrative-user")
                .addComp(warning())
                .hide(showAdminWarning.not())
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
                systemState.setValue(null);
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(busy, () -> {
                    var sc = newValue.getStore().getOrStartSession();
                    systemState.setValue(SystemState.of(sc, identity));
                });
            });
        });

        var systemChoice = new StoreChoiceComp<>(null, system, ShellStore.class, null, StoreViewState.get().getAllConnectionsCategory()) {

            @Override
            protected String toName(DataStoreEntry entry) {
                if (entry == null) {
                    return null;
                }

                return DataStorage.get().getStoreEntryDisplayName(entry) + " -> " + IdentitySummary.createSummary(identity);
            }
        };
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
