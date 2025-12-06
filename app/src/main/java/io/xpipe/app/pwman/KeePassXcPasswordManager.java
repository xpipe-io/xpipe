package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.ListBoxViewComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.DerivedObservableList;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.*;
import io.xpipe.core.OsType;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.property.Property;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javafx.collections.FXCollections;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

@Getter
@Builder(toBuilder = true)
@ToString
@Jacksonized
@JsonTypeName("keePassXc")
public class KeePassXcPasswordManager implements PasswordManager {

    private static KeePassXcProxyClient client;

    private final List<KeePassXcAssociationKey> associationKeys;

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<KeePassXcPasswordManager> p) {
        var prop = FXCollections.<KeePassXcAssociationKey>observableArrayList();
        p.subscribe(keePassXcManager -> {
            DerivedObservableList.wrap(prop, true).setContent(keePassXcManager != null && keePassXcManager.getAssociationKeys() != null ? keePassXcManager.getAssociationKeys() : List.of());
        });

        var associationsListComp = new ListBoxViewComp<>(prop, prop, k -> new KeePassXcAssociationComp(k, () -> prop.remove(k)), false);

        return new OptionsBuilder()
                .nameAndDescription("keePassXcNotAssociated")
                .addComp(new ButtonComp(AppI18n.observable("keePassXcNotAssociatedButton"), () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        var r = associate();
                        Platform.runLater(() -> {
                            prop.add(r);
                        });
                    });
                }))
                .hide(Bindings.isNotEmpty(prop))
                .nameAndDescription("keePassXcAssociated")
                .addComp(associationsListComp)
                .hide(Bindings.isEmpty(prop))
                .nameAndDescription("keePassXcAssociateMore")
                .addComp(new ButtonComp(AppI18n.observable("keePassXcNotAssociatedButton"), () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        var r = associate();
                        Platform.runLater(() -> {
                            prop.add(r);
                        });
                    });
                }))
                .hide(Bindings.isEmpty(prop))

                .addProperty(prop)
                .bind(
                        () -> {
                            return new KeePassXcPasswordManager(prop);
                        },
                        p);
    }

    private static KeePassXcAssociationKey associate() throws IOException {
        var found = findKeePassProxy();
        if (found.isEmpty()) {
            throw ErrorEventFactory.expected(new UnsupportedOperationException("No KeePassXC installation was found"));
        }

        var c = new KeePassXcProxyClient(found.get());
        try {
            c.connect();
            c.exchangeKeys();
            var key = c.associate();
            c.testAssociation(key);
            return key;
        } finally {
            c.disconnect();
        }
    }

    public static void reset() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    private static synchronized KeePassXcProxyClient getOrCreate() throws Exception {
        if (client == null) {
            var found = findKeePassProxy();
            if (found.isEmpty()) {
                throw ErrorEventFactory.expected(
                        new UnsupportedOperationException("No KeePassXC installation was found"));
            }

            var c = new KeePassXcProxyClient(found.get());
            c.connect();
            c.exchangeKeys();
            var pref = AppPrefs.get().passwordManager();

            var available = pref.getValue() instanceof KeePassXcPasswordManager kpm ? kpm.getAssociationKeys() : null;
            if (available == null) {
                available = new ArrayList<>();
            }

            if (!available.isEmpty()) {
                var valid = false;
                Exception first = null;
                for (KeePassXcAssociationKey key : new ArrayList<>(available)) {
                    try {
                        c.testAssociation(key);
                        valid = true;
                    } catch (Exception e) {
                        if (first == null) {
                            first = e;
                        }
                    }
                }

                // Only one association needs to work
                if (!valid) {
                    ErrorEventFactory.preconfigure(ErrorEventFactory.fromThrowable(first)
                            .description("KeePassXC association for " + available.getFirst().getKey() + " failed")
                            .expected());
                    throw first;
                }
            } else {
                var key = c.associate();
                c.testAssociation(key);
                if (pref.getValue() instanceof KeePassXcPasswordManager kpm) {
                    AppPrefs.get()
                            .setFromExternal(
                                    AppPrefs.get().passwordManager(),
                                    kpm.toBuilder()
                                            .associationKeys(List.of(key))
                                            .build());
                }
            }

            client = c;
        }

        return client;
    }

    private static Optional<Path> findKeePassProxy() throws IOException {
        try (var sc = LocalShell.getShell().start()) {
            var found = sc.view().findProgram("keepassxc-proxy").map(filePath -> filePath.asLocalPath());
            if (found.isPresent()) {
                // Symlinks don't work with the proxy
                return Optional.of(found.get().toRealPath());
            }
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }

        Optional<Path> found =
                switch (OsType.ofLocal()) {
                    case OsType.Linux ignored -> {
                        var paths = List.of(
                                Path.of("/usr/bin/keepassxc-proxy"),
                                Path.of("/usr/local/bin/keepassxc-proxy"),
                                Path.of("/snap/keepassxc/current/usr/bin/keepassxc-proxy"));
                        yield paths.stream().filter(path -> Files.exists(path)).findFirst();
                    }
                    case OsType.MacOs ignored -> {
                        var paths = List.of(Path.of("/Applications/KeePassXC.app/Contents/MacOS/keepassxc-proxy"));
                        yield paths.stream().filter(path -> Files.exists(path)).findFirst();
                    }
                    case OsType.Windows ignored -> {
                        try {
                            var foundKey = WindowsRegistry.local()
                                    .findKeyForEqualValueMatchRecursive(
                                            WindowsRegistry.HKEY_LOCAL_MACHINE,
                                            "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                                            "https://keepassxc.org");
                            if (foundKey.isPresent()) {
                                var installKey = WindowsRegistry.local()
                                        .readStringValueIfPresent(
                                                foundKey.get().getHkey(),
                                                foundKey.get().getKey(),
                                                "InstallLocation");
                                if (installKey.isPresent()) {
                                    yield installKey
                                            .map(p -> p + "\\keepassxc-proxy.exe")
                                            .map(Path::of);
                                }
                            }
                        } catch (Exception e) {
                            ErrorEventFactory.fromThrowable(e).handle();
                        }
                        yield Optional.empty();
                    }
                };
        if (found.isEmpty()) {
            return Optional.empty();
        }

        // Symlinks don't work with the proxy
        var real = found.get().toRealPath();
        return Optional.of(real);
    }

    @Override
    public CredentialResult retrieveCredentials(String key) {
        try {
            var hasScheme = Pattern.compile("^\\w+://").matcher(key).find();
            var fixedKey = hasScheme ? key : "https://" + key;
            var client = getOrCreate();
            var credentials = client.getCredentials(associationKeys, fixedKey);
            return credentials;
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("keePassXcPlaceholder");
    }

    @Override
    public String getWebsite() {
        return "https://keepassxc.org/";
    }
}
