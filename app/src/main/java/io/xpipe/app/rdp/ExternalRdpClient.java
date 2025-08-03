package io.xpipe.app.rdp;

import io.xpipe.app.ext.PrefsChoiceValue;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.util.*;
import io.xpipe.core.OsType;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.function.Supplier;

public interface ExternalRdpClient extends PrefsChoiceValue {

    static ExternalRdpClient getApplicationLauncher() {
        if (OsType.getLocal() == OsType.WINDOWS) {
            return MSTSC;
        } else {
            return AppPrefs.get().rdpClientType().getValue();
        }
    }

    ExternalRdpClient MSTSC = new MstscRdpClient();

    ExternalRdpClient DEVOLUTIONS = new DevolutionsRdpClient();

    ExternalRdpClient REMMINA = new RemminaRdpClient();

    ExternalRdpClient X_FREE_RDP = new FreeRdpClient();

    ExternalRdpClient MICROSOFT_REMOTE_DESKTOP_MACOS_APP = new RemoteDesktopAppRdpClient();

    ExternalRdpClient WINDOWS_APP_MACOS = new WindowsAppRdpClient();

    ExternalRdpClient CUSTOM = new CustomRdpClient();
    List<ExternalRdpClient> WINDOWS_CLIENTS = List.of(MSTSC, DEVOLUTIONS);
    List<ExternalRdpClient> LINUX_CLIENTS = List.of(REMMINA, X_FREE_RDP);
    List<ExternalRdpClient> MACOS_CLIENTS = List.of(X_FREE_RDP, MICROSOFT_REMOTE_DESKTOP_MACOS_APP, WINDOWS_APP_MACOS);

    @SuppressWarnings("TrivialFunctionalExpressionUsage")
    List<ExternalRdpClient> ALL = ((Supplier<List<ExternalRdpClient>>) () -> {
                var all = new ArrayList<ExternalRdpClient>();
                if (OsType.getLocal().equals(OsType.WINDOWS)) {
                    all.addAll(WINDOWS_CLIENTS);
                }
                if (OsType.getLocal().equals(OsType.LINUX)) {
                    all.addAll(LINUX_CLIENTS);
                }
                if (OsType.getLocal().equals(OsType.MACOS)) {
                    all.addAll(MACOS_CLIENTS);
                }
                all.add(CUSTOM);
                return all;
            })
            .get();

    static ExternalRdpClient determineDefault(ExternalRdpClient existing) {
        // Verify that our selection is still valid
        if (existing != null && existing.isAvailable()) {
            return existing;
        }

        var r = ALL.stream()
                .filter(t -> !t.equals(CUSTOM))
                .filter(t -> t.isAvailable())
                .findFirst()
                .orElse(null);

        // Check if detection failed for some reason
        if (r == null) {
            var def = OsType.getLocal() == OsType.WINDOWS
                    ? MSTSC
                    : OsType.getLocal() == OsType.MACOS ? WINDOWS_APP_MACOS : REMMINA;
            r = def;
        }

        return r;
    }

    void launch(RdpLaunchConfig configuration) throws Exception;

    boolean supportsPasswordPassing();

    String getWebsite();

    default Path writeRdpConfigFile(String title, RdpConfig input) throws Exception {
        var name = OsFileSystem.ofLocal().makeFileSystemCompatible(title);
        var file = ShellTemp.getLocalTempDataDirectory("rdp").resolve(name + ".rdp");
        var string = input.toString();
        Files.createDirectories(file.getParent());
        Files.writeString(file, string);
        return file;
    }
}
