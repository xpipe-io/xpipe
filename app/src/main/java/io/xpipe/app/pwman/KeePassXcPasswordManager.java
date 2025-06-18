package io.xpipe.app.pwman;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.*;
import io.xpipe.core.process.OsType;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
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

    private final KeePassXcAssociationKey associationKey;

    public static OptionsBuilder createOptions(Property<KeePassXcPasswordManager> p) {
        var prop = new SimpleObjectProperty<KeePassXcAssociationKey>();
        p.subscribe(keePassXcManager -> {
            prop.set(keePassXcManager != null ? keePassXcManager.getAssociationKey() : null);
        });
        return new OptionsBuilder()
                .nameAndDescription("keePassXcNotAssociated")
                .addComp(new ButtonComp(AppI18n.observable("keePassXcNotAssociatedButton"), () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        var r = associate();
                        prop.setValue(r);
                    });
                }))
                .hide(prop.isNotNull())
                .nameAndDescription("keePassXcAssociated")
                .addComp(new OptionsBuilder()
                        .name("identifier")
                        .addStaticString(prop.map(k -> k.getId()))
                        .name("key")
                        .addStaticString(prop.map(k -> {
                            var s = k.getKey().getSecretValue();
                            return s.substring(0, 6) + "*".repeat(s.length() - 6);
                        }))
                        .buildComp()
                        .maxWidth(600))
                .hide(prop.isNull())
                .addProperty(prop)
                .bind(
                        () -> {
                            return new KeePassXcPasswordManager(prop.getValue());
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
            c.associate();
            c.testAssociation();
            return c.getAssociationKey();
        } finally {
            c.disconnect();
        }
    }

    private static CredentialResult receive(String key) throws Exception {
        var hasScheme = Pattern.compile("^\\w+://").matcher(key).find();
        var fixedKey = hasScheme ? key : "https://" + key;
        var client = getOrCreate();
        var response = client.getLoginsMessage(fixedKey);
        var credentials = client.getCredentials(response);
        return credentials;
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
            KeePassXcAssociationKey cached =
                    pref.getValue() instanceof KeePassXcPasswordManager kpm ? kpm.getAssociationKey() : null;
            if (cached != null) {
                c.useExistingAssociationKey(cached);
                try {
                    c.testAssociation();
                } catch (Exception e) {
                    ErrorEventFactory.fromThrowable(e).handle();
                    c.useExistingAssociationKey(null);
                    cached = null;
                }
            }
            if (cached == null) {
                c.associate();
                c.testAssociation();
                if (pref.getValue() instanceof KeePassXcPasswordManager kpm
                        && !c.getAssociationKey().equals(kpm.getAssociationKey())) {
                    AppPrefs.get()
                            .setFromExternal(
                                    AppPrefs.get().passwordManager(),
                                    kpm.toBuilder()
                                            .associationKey(c.getAssociationKey())
                                            .build());
                }
            }
            client = c;
        }

        return client;
    }

    private static Optional<Path> findKeePassProxy() {
        try (var sc = LocalShell.getShell().start()) {
            var found = sc.view().findProgram("keepassxc-proxy");
            if (found.isPresent()) {
                return found.map(filePath -> filePath.asLocalPath());
            }

        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
        }

        return switch (OsType.getLocal()) {
            case OsType.Linux linux -> {
                var paths = List.of(Path.of("/usr/bin/keepassxc-proxy"), Path.of("/usr/local/bin/keepassxc-proxy"));
                yield paths.stream().filter(path -> Files.exists(path)).findFirst();
            }
            case OsType.MacOs macOs -> {
                var paths = List.of(Path.of("/Applications/KeePassXC.app/Contents/MacOS/keepassxc-proxy"));
                yield paths.stream().filter(path -> Files.exists(path)).findFirst();
            }
            case OsType.Windows windows -> {
                try {
                    var foundKey = WindowsRegistry.local()
                            .findKeyForEqualValueMatchRecursive(
                                    WindowsRegistry.HKEY_LOCAL_MACHINE,
                                    "SOFTWARE\\Microsoft\\Windows\\CurrentVersion\\Uninstall",
                                    "https://keepassxc.org");
                    if (foundKey.isPresent()) {
                        var installKey = WindowsRegistry.local()
                                .readStringValueIfPresent(
                                        foundKey.get().getHkey(), foundKey.get().getKey(), "InstallLocation");
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
    }

    @Override
    public CredentialResult retrieveCredentials(String key) {
        try {
            return KeePassXcPasswordManager.receive(key);
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return null;
        }
    }

    @Override
    public String getKeyPlaceholder() {
        return AppI18n.get("keePassXcPlaceholder");
    }
}
