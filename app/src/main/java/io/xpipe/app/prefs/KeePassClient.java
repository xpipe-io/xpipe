package io.xpipe.app.prefs;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.OsType;
import lombok.SneakyThrows;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public class KeePassClient {

    private static KeePassNativeClient client;

    @SneakyThrows
    public static String receive(String key) {
        var client = getOrCreate();
        client.getDatabaseGroups();
        return client.getLogins("abc");
    }

    public static void reset() {
        if (client != null) {
            client.disconnect();
            client = null;
        }
    }

    private static synchronized KeePassNativeClient getOrCreate() throws Exception {
        if (client == null) {
            var found = findKeePassProxy();
            if (found.isEmpty()) {
                throw ErrorEvent.expected(new UnsupportedOperationException("No KeePassXC installation was found"));
            }

            var c = new KeePassNativeClient(found.get());
            c.connect();
            c.exchangeKeys();
            KeePassAssociationKey cached = AppCache.getNonNull("keepassxc-association", KeePassAssociationKey.class, () -> null);
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
}
