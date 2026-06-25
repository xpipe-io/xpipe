package io.xpipe.app.rdp;

import com.sun.jna.LastErrorException;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.Pointer;
import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppDisplayScale;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.platform.ClipboardHelper;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.*;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.io.FileUtils;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;

import static io.xpipe.app.util.CredAdvapi32.*;

@JsonTypeName("mstsc")
@Value
@Jacksonized
@Builder
public class MstscRdpClient implements ExternalApplicationType.PathApplication, ExternalRdpClient {

    private static Boolean usesNewSecurityDialog = null;

    private static synchronized boolean usesNewSecurityDialog() {
        if (usesNewSecurityDialog != null) {
            return usesNewSecurityDialog;
        }

        if (OsType.ofLocal() != OsType.WINDOWS) {
            return (usesNewSecurityDialog = false);
        }

        var build = WindowsRegistry.local()
                .readStringValueIfPresent(
                        WindowsRegistry.HKEY_LOCAL_MACHINE,
                        "SOFTWARE\\Microsoft\\Windows NT\\CurrentVersion",
                        "CurrentBuild");
        if (build.isEmpty()) {
            return (usesNewSecurityDialog = false);
        }

        return (usesNewSecurityDialog = ("26200".equals(build.get())));
    }

    private static boolean isNewSecurityDialogEnabled() {
        var version = WindowsRegistry.local()
                .readIntegerValueIfPresent(
                        WindowsRegistry.HKEY_LOCAL_MACHINE,
                        "SOFTWARE\\Policies\\Microsoft\\Windows NT\\Terminal Services\\Client",
                        "RedirectionWarningDialogVersion");
        return version.isEmpty() || version.getAsInt() != 1;
    }

    private static synchronized void changeSecurityDialogSetting(boolean val) throws Exception {
        var sc = LocalShell.getLocalPowershell();
        if (sc.isEmpty()) {
            return;
        }

        if (val) {
            sc.get()
                    .command(
                            "Start-Process reg -Wait -ArgumentList add, \"`\"HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows NT\\Terminal Services\\Client`\"\", /t, REG_DWORD , /v, RedirectionWarningDialogVersion, /d, 1, /f -Verb runAs")
                    .executeAndCheck();
        } else {
            sc.get()
                    .command(
                            "Start-Process reg -Wait -ArgumentList delete, \"`\"HKLM\\SOFTWARE\\Policies\\Microsoft\\Windows NT\\Terminal Services\\Client`\"\", /v, RedirectionWarningDialogVersion, /f -Verb runAs")
                    .executeAndCheck();
        }
    }

    @Value
    @Jacksonized
    @Builder
    public static class RegistryCache {
        String usernameHint;
        byte[] certHash;
    }

    private static int launchCounter = 0;

    @SuppressWarnings("unused")
    static OptionsBuilder createOptions(Property<MstscRdpClient> property) {
        var dock = new SimpleObjectProperty<>(property.getValue().isDock());
        var smartSizing = new SimpleObjectProperty<>(property.getValue().isSmartSizing());
        var useSystemDisplayScale =
                new SimpleBooleanProperty(property.getValue().isUseSystemDisplayScale());

        var rdpSecurityValueHide = new SimpleBooleanProperty();
        var rdpSecurityValue = new SimpleBooleanProperty();
        ThreadHelper.runAsync(() -> {
            var val = MstscRdpClient.usesNewSecurityDialog();
            rdpSecurityValueHide.set(!val);

            Platform.runLater(() -> {
                rdpSecurityValue.set(!MstscRdpClient.isNewSecurityDialogEnabled());
                rdpSecurityValue.addListener((observable, oldValue, newValue) -> {
                    ThreadHelper.runFailableAsync(() -> {
                        MstscRdpClient.changeSecurityDialogSetting(newValue);
                    });
                });
            });
        });

        return new OptionsBuilder()
                .nameAndDescription("rdpDock")
                .addToggle(dock)
                .nameAndDescription("rdpSmartSizing")
                .addToggle(smartSizing)
                .hide(dock)
                .nameAndDescription("rdpUseSystemDisplayScale")
                .addToggle(useSystemDisplayScale)
                .hide(AppDisplayScale.hasOnlyDefaultDisplayScale())
                .nameAndDescription("disableRdpWindowsSecurityWarning")
                .addToggle(rdpSecurityValue)
                .hide(rdpSecurityValueHide)
                .bind(
                        () -> MstscRdpClient.builder()
                                .dock(dock.get())
                                .smartSizing(smartSizing.get())
                                .useSystemDisplayScale(useSystemDisplayScale.get())
                                .build(),
                        property);
    }

    boolean dock;
    boolean smartSizing;
    boolean useSystemDisplayScale;

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var securityDialogShown = AppCache.getBoolean("rdpWindowsSecurityWarningDialog", false);
        if (!securityDialogShown && usesNewSecurityDialog() && isNewSecurityDialogEnabled()) {
            var modal = ModalOverlay.of(
                    "rdpWindowsSecurityWarningDialogTitle",
                    AppDialog.dialogTextKey("rdpWindowsSecurityWarningDialogContent"));
            modal.addButton(ModalButton.cancel());
            modal.addButton(new ModalButton(
                    "openSettings",
                    () -> {
                        AppPrefs.get().selectCategory("rdp");
                    },
                    true,
                    true));
            modal.show();
            AppCache.update("rdpWindowsSecurityWarningDialog", true);
        }

        var adaptedRdpConfig = configuration.isRemoteApp()
                ? getAdaptedConfig(configuration)
                : getRemoteDesktopWindowConfig(getAdaptedConfig(configuration));
        var window = RemoteDesktopWindow.get();
        String width = null;
        String height = null;
        if (!configuration.isRemoteApp() && dock && window != null) {
            window.show();

            var factor = useSystemDisplayScale ? AppDisplayScale.getEffectiveDisplayScale() : 1.0;
            width = "/w:" + Math.round(window.getDockBounds().getW() / factor);
            height = "/h:" + Math.round(window.getDockBounds().getH() / factor);
        }
        var setCache = prepareLocalhostRegistryCache(configuration);

        disableSignatureWarning(configuration);

        if (configuration.getPassword() != null) {
            WinCred.setCredential("TERMSRV/" + configuration.getHost(), CRED_TYPE_DOMAIN_PASSWORD, CRED_PERSIST_SESSION,
                    configuration.getUsername(), configuration.getPassword());
        }

        var gateway = configuration.getGateway();
        if (gateway != null && gateway.getPassword() != null) {
            WinCred.setCredential(gateway.getHost(), CRED_TYPE_DOMAIN_PASSWORD, CRED_PERSIST_SESSION,
                    gateway.getUsername(), gateway.getPassword());
        }

        var file = writeRdpConfigFile(configuration.getTitle(), adaptedRdpConfig);
        var process = LocalExec.executeAsync(getExecutable(), file.toString(), width, height);
        if (process != null && window != null && !configuration.isRemoteApp() && dock) {
            window.show();
            var entry = configuration.getEntry();
            window.trackExternal(
                    configuration.getTitle(),
                    entry.getEffectiveIconFile(),
                    DataStorage.get().getEffectiveColor(entry),
                    entry,
                    window.getDockBounds().getW(),
                    window.getDockBounds().getH(),
                    process,
                    Duration.ofSeconds(120),
                    p -> {
                        return !p.isDialog();
                    });
        }

        if (!setCache) {
            var localhost = configuration
                    .getHost()
                    .startsWith("localhost");
            if (localhost) {
                saveLocalhostRegistryCache(configuration.getStoreId());
            }
        }
    }

    @Override
    public boolean supportsPasswordPassing(RdpLaunchConfig config) {
        return true;
    }

    @Override
    public String getWebsite() {
        return "https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/mstsc";
    }

    private RdpConfig getRemoteDesktopWindowConfig(RdpConfig input) {
        if (useSystemDisplayScale) {
            input = input.overlay(Map.of("desktopscalefactor", new RdpConfig.TypedValue("i", "200")));
        }

        var window = RemoteDesktopWindow.get();
        if (dock && window != null) {
            window.show();
            var s = window.getDockBounds();
            if (s != null) {
                var pos =
                        "0,1," + s.getX() + "," + s.getY() + "," + (s.getX() + s.getW()) + "," + (s.getY() + s.getH());
                var adapted = input.overlay(Map.of(
                        "winposstr", new RdpConfig.TypedValue("s", pos),
                        "pinconnectionbar", new RdpConfig.TypedValue("i", "0"),
                        "displayconnectionbar", new RdpConfig.TypedValue("i", "0"),
                        "screen mode id", new RdpConfig.TypedValue("i", "1"),
                        "use multimon", new RdpConfig.TypedValue("i", "0"),
                        "smart sizing", new RdpConfig.TypedValue("i", "1")));
                return adapted;
            }
        }

        return input;
    }

    private RdpConfig getAdaptedConfig(RdpLaunchConfig configuration) {
        var input = configuration.getConfig();
        var pass = configuration.getPassword();
        var adapted = input.overlay(Map.of(
                "prompt for credentials",
                new RdpConfig.TypedValue("i", pass != null ? "0" : "1"),
                "smart sizing",
                new RdpConfig.TypedValue("i", smartSizing ? "1" : "0")));
        return adapted;
    }

    private void disableSignatureWarning(RdpLaunchConfig config) {
        WindowsRegistry.local()
                .setIntegerValue(
                        WindowsRegistry.HKEY_CURRENT_USER,
                        "Software\\Microsoft\\Terminal Server Client\\LocalDevices",
                        config.getHost(),
                        0x4c);
        if (config.getGateway() != null) {
            WindowsRegistry.local()
                    .setIntegerValue(
                            WindowsRegistry.HKEY_CURRENT_USER,
                            "Software\\Microsoft\\Terminal Server Client\\LocalDevices",
                            config.getHost() + ";" + config.getGateway().getHost(),
                            0x4c);
        }
    }

    private void saveLocalhostRegistryCache(UUID entry) {
        var counter = ++launchCounter;
        var attempts = new AtomicInteger();
        GlobalTimer.scheduleUntil(Duration.ofSeconds(1), false, () -> {
            if (counter != launchCounter || attempts.getAndIncrement() > 15) {
                return true;
            }

            var ex = WindowsRegistry.local()
                    .keyExists(
                            WindowsRegistry.HKEY_CURRENT_USER,
                            "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost");
            if (!ex) {
                return false;
            }

            var user = WindowsRegistry.local()
                    .readStringValueIfPresent(
                            WindowsRegistry.HKEY_CURRENT_USER,
                            "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost",
                            "UsernameHint")
                    .orElse(null);
            var cert = WindowsRegistry.local()
                    .readBinaryValueIfPresent(
                            WindowsRegistry.HKEY_CURRENT_USER,
                            "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost",
                            "CertHash")
                    .orElse(null);
            if (user == null && cert == null) {
                return true;
            }

            AppCache.update(
                    "rdp-" + entry,
                    RegistryCache.builder().usernameHint(user).certHash(cert).build());
            return true;
        });
    }

    private Optional<RegistryCache> getLocalhostRegistryCache(UUID entry) {
        RegistryCache found = AppCache.getNonNull("rdp-" + entry, RegistryCache.class, () -> null);
        return Optional.ofNullable(found);
    }

    private boolean prepareLocalhostRegistryCache(RdpLaunchConfig configuration) {
        WindowsRegistry.local()
                .deleteKey(
                        WindowsRegistry.HKEY_CURRENT_USER,
                        "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost");


        var localhost = configuration.getHost().startsWith("localhost");
        if (localhost) {
            var found = getLocalhostRegistryCache(configuration.getStoreId());
            if (found.isPresent()) {
                var user = found.get().getUsernameHint();
                if (user != null) {
                    WindowsRegistry.local()
                            .setStringValue(
                                    WindowsRegistry.HKEY_CURRENT_USER,
                                    "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost",
                                    "UsernameHint",
                                    user);
                }

                var cert = found.get().getCertHash();
                if (cert != null) {
                    WindowsRegistry.local()
                            .setBinaryValue(
                                    WindowsRegistry.HKEY_CURRENT_USER,
                                    "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost",
                                    "CertHash",
                                    cert);
                }

                return user != null || cert != null;
            }
        }

        return false;
    }

    @Override
    public String getExecutable() {
        return "mstsc.exe";
    }

    @Override
    public boolean detach() {
        return false;
    }
}
