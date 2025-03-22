package io.xpipe.app.prefs;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.*;
import io.xpipe.core.process.OsType;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

@Getter
@Builder
@Jacksonized
@JsonTypeName("keePassXc")
public class KeePassXcManager implements PasswordManager {

    private static KeePassXcProxyClient client;

    private final KeePassXcAssociationKey associationKey;

    public static OptionsBuilder createOptions(Property<KeePassXcManager> p) {
        var prop = new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getAssociationKey() : null);
        return new OptionsBuilder()
                .nameAndDescription("keePassXcNotAssociated")
                .addComp(new ButtonComp(AppI18n.observable("keePassXcNotAssociatedButton"), () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        var r = associate();
                        prop.setValue(r);
                    });
                }))
                .hide(prop.isNotNull())
                .name("abc")
                .addStaticString(prop.map(k -> k.getId()))
                .hide(prop.isNull())
                .nameAndDescription("keePassXcAssociated")
                .addComp(new ButtonComp(AppI18n.observable("keePassXcNotAssociatedButton"), () -> {
                    ThreadHelper.runFailableAsync(() -> {
                        var r = associate();
                        prop.setValue(r);
                    });
                }))
                .hide(prop.isNull())
                .bind(() -> {
                    return new KeePassXcManager(prop.getValue());
                }, p);
    }

    private static KeePassXcAssociationKey associate() throws IOException {
        var found = findKeePassProxy();
        if (found.isEmpty()) {
            throw ErrorEvent.expected(new UnsupportedOperationException("No KeePassXC installation was found"));
        }

        var c = new KeePassXcProxyClient(found.get());
        try {
            c.connect();
            c.exchangeKeys();
            c.associate();
            c.testAssociation();
            return c.getAssociationKey();
        } catch (Exception e) {
            c.disconnect();
            throw e;
        }
    }

    private static String receive(String key) throws Exception {
        var fixedKey = key.startsWith("http://") || key.startsWith("https://") ? key : "https://" + key;
        var client = getOrCreate();
        var response = client.getLoginsMessage(fixedKey);
        var password = client.getPassword(response);
        return password;
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
                throw ErrorEvent.expected(new UnsupportedOperationException("No KeePassXC installation was found"));
            }

            var c = new KeePassXcProxyClient(found.get());
            c.connect();
            c.exchangeKeys();
            KeePassXcAssociationKey cached = AppCache.getNonNull("keepassxc-association", KeePassXcAssociationKey.class, () -> null);
            if (cached != null) {
                c.useExistingAssociationKey(cached);
                try {
                    c.testAssociation();
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).handle();
                    c.useExistingAssociationKey(null);
                    cached = null;
                }
            }
            if (cached == null) {
                c.associate();
                c.testAssociation();
                AppCache.update("keepassxc-association", c.getAssociationKey());
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
            ErrorEvent.fromThrowable(e).handle();
        }

        return switch (OsType.getLocal()) {
            case OsType.Linux linux -> {
                var paths = List.of(Path.of("/usr/bin/keepassxc-proxy"), Path.of("/usr/local/bin/keepassxc-proxy"));
                yield  paths.stream().filter(path -> Files.exists(path)).findFirst();
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
                            yield installKey.map(p -> p + "\\keepassxc-proxy.exe").map(Path::of);
                        }
                    }
                } catch (Exception e) {
                    ErrorEvent.fromThrowable(e).handle();
                }
                yield Optional.empty();
            }
        };
    }

    @Override
    public String getDocsLink() {
        return DocumentationLink.KEEPASSXC.getLink();
    }

    @Override
    public String retrievePassword(String key) {
        try {
            return KeePassXcManager.receive(key);
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).handle();
            return null;
        }
    }
}
