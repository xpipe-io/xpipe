package io.xpipe.ext.base.identity.ssh;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.hub.comp.StoreChoiceComp;
import io.xpipe.app.hub.comp.StoreViewState;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellView;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.BooleanScope;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.FilePath;
import io.xpipe.ext.base.identity.IdentityStore;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Data;
import lombok.Value;

import java.util.Optional;

public class IdentityExportDialog {

    @Data
    private static class SystemState {

        boolean inAuthorizedKeys;
        boolean keyAuthEnabled;
        boolean keyAuthInMethods;
        boolean passwordAuthEnabled;
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
            var configFile = FilePath.of("etc", "ssh", "sshd_config");
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


    public static void show(IdentityStore identity) {
        var busy = new SimpleBooleanProperty();
        var system = new SimpleObjectProperty<DataStoreEntryRef<ShellStore>>();
        system.addListener((observable, oldValue, newValue) -> {
            if (newValue == null) {
                return;
            }

            ThreadHelper.runFailableAsync(() -> {
                BooleanScope.executeExclusive(busy, () -> {
                    try (var sc = newValue.getStore().standaloneControl().start()) {
                    }
                });
            });
        });

        var authorizedHost = new SimpleBooleanProperty(true);

        var systemChoice = new StoreChoiceComp<>(null, null, ShellStore.class, null, StoreViewState.get().getAllConnectionsCategory());
        var options = new OptionsBuilder()
                .nameAndDescription("identityExportTargetHost")
                .addComp(systemChoice, system)
                .nameAndDescription("identityExportAuthorizedHost")
                .addToggle(authorizedHost);

        var modal = ModalOverlay.of("identityExportTitle", options.buildComp());
        modal.addButton(ModalButton.cancel());
        modal.addButton(ModalButton.ok(() -> {

        }));
        modal.show();
    }
}
