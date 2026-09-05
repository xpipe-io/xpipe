package io.xpipe.app.rdp;

import io.xpipe.app.core.AppLocalTemp;
import io.xpipe.app.prefs.*;
import io.xpipe.app.process.OsFileSystem;
import io.xpipe.app.util.*;

import com.fasterxml.jackson.annotation.JsonTypeInfo;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
public interface ExternalRdpClient extends PrefsValue, PrefsCapabilityProvider {

    static List<Class<?>> getClasses() {
        var l = new ArrayList<Class<?>>();
        switch (OsType.ofLocal()) {
            case OsType.Linux ignored -> {
                l.add(RemminaRdpClient.class);
                l.add(FreeRdpClient.class);
                l.add(KrdcRdpClient.class);
            }
            case OsType.MacOs ignored -> {
                l.add(RemoteDesktopAppRdpClient.class);
                l.add(WindowsAppRdpClient.class);
                l.add(FreeRdpClient.class);
            }
            case OsType.Windows ignored -> {
                // l.add(MsrdcRdpClient.class);
                l.add(MstscRdpClient.class);
                l.add(DevolutionsRdpClient.class);
            }
        }
        l.add(CustomRdpClient.class);
        return l;
    }

    static ExternalRdpClient getApplicationLauncher() {
        if (OsType.ofLocal() == OsType.WINDOWS) {
            var msrdc = AppPrefs.get().rdpClientType().getValue() instanceof MsrdcRdpClient;
            return msrdc
                    ? MsrdcRdpClient.builder().smartSizing(false).build()
                    : MstscRdpClient.builder().smartSizing(false).build();
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
                var freeRdp = new FreeRdpClient(null);
                var remmina = new RemminaRdpClient(null);
                var krdc = new KrdcRdpClient();
                yield remmina.isAvailable()
                        ? remmina
                        : freeRdp.isAvailable() ? freeRdp : krdc.isAvailable() ? krdc : remmina;
            }
            case OsType.MacOs ignored -> {
                var remoteDesktopApp = new RemoteDesktopAppRdpClient();
                if (remoteDesktopApp.isAvailable()) {
                    yield remoteDesktopApp;
                }

                var windowsApp = WindowsAppRdpClient.builder().hidpi(true).build();
                if (windowsApp.isAvailable()) {
                    yield windowsApp;
                }

                var freeRdp = new FreeRdpClient(null);
                if (freeRdp.isAvailable()) {
                    yield freeRdp;
                }

                yield windowsApp;
            }
            case OsType.Windows ignored -> {
                var msrdc =
                        MsrdcRdpClient.builder().smartSizing(true).dock(true).build();
                if (msrdc.isAvailable()) {
                    // yield msrdc;
                }

                var mstsc =
                        MstscRdpClient.builder().smartSizing(true).dock(true).build();
                if (mstsc.isAvailable()) {
                    yield mstsc;
                }

                yield mstsc;
            }
        };
    }

    @Override
    default PrefsCapabilities getCapabilities() {
        var passwords = supportsPasswordPassing();
        // var resize = true;
        var options = supportsAdditionalRdpOptions();
        return PrefsCapabilities.of(
                PrefsCapability.of("rdpCapabilityPasswordPassing", PrefsCapability.Type.of(passwords)),
                // PrefsCapability.of("rdpCapabilityScreenResize", PrefsCapability.Type.of(resize)),
                PrefsCapability.of("rdpCapabilityAdditionalOptions", PrefsCapability.Type.of(options))
        );
    }

    void launch(RdpLaunchConfig configuration) throws Exception;

    boolean supportsPasswordPassing();

    boolean supportsAdditionalRdpOptions();

    String getWebsite();

    default Path writeRdpConfigFile(String title, RdpConfig input) throws Exception {
        var name = OsFileSystem.ofLocal().makeFileSystemCompatible(title).replaceAll("\\s+", "_");
        var file = AppLocalTemp.getLocalTempDataDirectory("rdp").resolve(name + ".rdp");
        var string = input.toString() + "\n";
        Files.createDirectories(file.getParent());
        Files.writeString(file, string);
        return file;
    }
}
