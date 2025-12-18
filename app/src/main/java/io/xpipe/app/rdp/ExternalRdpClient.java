package io.xpipe.app.rdp;

import io.xpipe.app.ext.PrefsValue;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.process.ShellTemp;
import io.xpipe.app.util.*;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ExternalRdpClient extends PrefsValue {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        switch (OsType.ofLocal()) {
            case OsType.Linux ignored -> {
                l.add(RemminaRdpClient.class);
                l.add(FreeRdpClient.class);
            }
            case OsType.MacOs ignored -> {
                l.add(RemoteDesktopAppRdpClient.class);
                l.add(WindowsAppRdpClient.class);
                l.add(FreeRdpClient.class);
            }
            case OsType.Windows ignored -> {
                l.add(MstscRdpClient.class);
                l.add(DevolutionsRdpClient.class);
            }
        }
        l.add(CustomRdpClient.class);
        return l;
    }

    static ExternalRdpClient getApplicationLauncher() {
        if (OsType.ofLocal() == OsType.WINDOWS) {
            return MstscRdpClient.builder().smartSizing(false).build();
        } else {
            return AppPrefs.get().rdpClientType().getValue();
        }
    }

    static ExternalRdpClient determineDefault(ExternalRdpClient existing) {
        // Verify that our selection is still valid
        if (existing != null && existing.isAvailable()) {
            return existing;
        }

        return switch (OsType.ofLocal()) {
            case OsType.Linux ignored -> {
                var freeRdp = new FreeRdpClient();
                var remmina = new RemminaRdpClient();
                yield remmina.isAvailable() ? remmina : freeRdp.isAvailable() ? freeRdp : remmina;
            }
            case OsType.MacOs ignored -> {
                var remoteDesktopApp = new RemoteDesktopAppRdpClient();
                if (remoteDesktopApp.isAvailable()) {
                    yield remoteDesktopApp;
                }

                var windowsApp = new WindowsAppRdpClient();
                if (windowsApp.isAvailable()) {
                    yield windowsApp;
                }

                var freeRdp = new FreeRdpClient();
                if (freeRdp.isAvailable()) {
                    yield freeRdp;
                }

                yield windowsApp;
            }
            case OsType.Windows ignored -> {
                yield MstscRdpClient.builder().smartSizing(false).build();
            }
        };
    }

    void launch(RdpLaunchConfig configuration) throws Exception;

    boolean supportsPasswordPassing();

    String getWebsite();

    default Path writeRdpConfigFile(String title, RdpConfig input) throws Exception {
        var name = OsFileSystem.ofLocal().makeFileSystemCompatible(title);
        var file = ShellTemp.getLocalTempDataDirectory("rdp").resolve(name + ".rdp");
        var string = input.toString() + "\n";
        Files.createDirectories(file.getParent());
        Files.writeString(file, string);
        return file;
    }
}
