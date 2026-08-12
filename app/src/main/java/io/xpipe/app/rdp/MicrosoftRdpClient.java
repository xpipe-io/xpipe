package io.xpipe.app.rdp;

import io.xpipe.app.comp.base.ModalButton;
import io.xpipe.app.comp.base.ModalOverlay;
import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppDisplayScale;
import io.xpipe.app.core.window.AppDialog;
import io.xpipe.app.ext.AuthModuleProvider;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.*;

import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;

import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;


@SuperBuilder(toBuilder = true)
@Getter
public abstract class MicrosoftRdpClient implements ExternalApplicationType.InstallLocationType, ExternalRdpClient {

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
    static <T extends MicrosoftRdpClient> OptionsBuilder createSharedOptions(Property<T> property) {
        var dock = new SimpleObjectProperty<>(property.getValue().isDock());
        var smartSizing = new SimpleObjectProperty<>(property.getValue().isSmartSizing());
        var useSystemDisplayScale =
                new SimpleBooleanProperty(property.getValue().isUseSystemDisplayScale());

        var rdpSecurityValueHide = new SimpleBooleanProperty();
        var rdpSecurityValue = new SimpleBooleanProperty();
        ThreadHelper.runAsync(() -> {
            var val = MicrosoftRdpClient.usesNewSecurityDialog();
            rdpSecurityValueHide.set(!val);

            Platform.runLater(() -> {
                rdpSecurityValue.set(!MicrosoftRdpClient.isNewSecurityDialogEnabled());
                rdpSecurityValue.addListener((observable, oldValue, newValue) -> {
                    ThreadHelper.runFailableAsync(() -> {
                        MicrosoftRdpClient.changeSecurityDialogSetting(newValue);
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
                        () -> {
                            MicrosoftRdpClientBuilder<T, ?> b = LombokHelper.toBuilder(property.getValue());
                            return b.dock(dock.get())
                                    .smartSizing(smartSizing.get())
                                    .useSystemDisplayScale(useSystemDisplayScale.get())
                                    .build();
                        },
                        property);
    }

    private final boolean dock;
    private final boolean smartSizing;
    private final boolean useSystemDisplayScale;

    private static final int CRED_TYPE_GENERIC = 1;
    private static final int CRED_TYPE_DOMAIN_PASSWORD = 2;
    private static final int CRED_PERSIST_SESSION = 1;

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
            AuthModuleProvider.get().setWindowsCredential(
                    "TERMSRV/" + configuration.getHost(),
                    CRED_TYPE_DOMAIN_PASSWORD,
                    CRED_PERSIST_SESSION,
                    configuration.getUsername(),
                    configuration.getPassword());
        }

        var gateway = configuration.getGateway();
        if (gateway != null && gateway.getPassword() != null) {
            AuthModuleProvider.get().setWindowsCredential(
                    gateway.getHost(),
                    CRED_TYPE_DOMAIN_PASSWORD,
                    CRED_PERSIST_SESSION,
                    gateway.getUsername(),
                    gateway.getPassword());
        }

        var file = writeRdpConfigFile(configuration.getTitle(), adaptedRdpConfig);
        var process = LocalExec.executeAsync(findExecutable().toString(), file.toString(), width, height);
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
            var localhost = configuration.getHost().startsWith("localhost");
            if (localhost) {
                saveLocalhostRegistryCache(configuration.getStoreId());
            }
        }
    }

    @Override
    public boolean supportsPasswordPassing() {
        return true;
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
}
