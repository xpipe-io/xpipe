package io.xpipe.app.rdp;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.RdpConfig;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.SecretValue;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.io.FileUtils;

import java.time.Duration;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@JsonTypeName("mstsc")
@Value
@Jacksonized
@Builder
public class MstscRdpClient implements ExternalApplicationType.PathApplication, ExternalRdpClient {

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
        var smartSizing = new SimpleObjectProperty<>(property.getValue().isSmartSizing());
        return new OptionsBuilder()
                .nameAndDescription("rdpSmartSizing")
                .addToggle(smartSizing)
                .bind(
                        () -> MstscRdpClient.builder()
                                .smartSizing(smartSizing.get())
                                .build(),
                        property);
    }

    boolean smartSizing;

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var adaptedRdpConfig = getAdaptedConfig(configuration);

        prepareLocalhostRegistryCache(configuration);

        var file = writeRdpConfigFile(configuration.getTitle(), adaptedRdpConfig);
        LocalShell.getShell()
                .command(CommandBuilder.of().add(getExecutable()).addFile(file.toString())).execute();

        GlobalTimer.delay(() -> {
            FileUtils.deleteQuietly(file.toFile());
        }, Duration.ofSeconds(1));

        var localhost = configuration.getConfig().get("full address").orElseThrow().getValue().startsWith("localhost");
        if (localhost) {
            var counter = ++launchCounter;
            GlobalTimer.delay(() -> {
                if (counter != launchCounter) {
                    return;
                }

                saveLocalhostRegistryCache(configuration.getStoreId());
            }, Duration.ofSeconds(15));
        }
    }

    @Override
    public boolean supportsPasswordPassing() {
        return LocalShell.getLocalPowershell().isPresent();
    }

    @Override
    public String getWebsite() {
        return "https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/mstsc";
    }

    private RdpConfig getAdaptedConfig(RdpLaunchConfig configuration) throws Exception {
        var input = configuration.getConfig();
        var pass = configuration.getPassword();
        if (input.get("password 51").isPresent() || !supportsPasswordPassing() || pass == null) {
            return input.overlay(Map.of("smart sizing", new RdpConfig.TypedValue("i", smartSizing ? "1" : "0")));
        }

        var adapted = input.overlay(Map.of(
                "password 51",
                new RdpConfig.TypedValue("b", encrypt(pass)),
                "prompt for credentials",
                new RdpConfig.TypedValue("i", "0"),
                "smart sizing",
                new RdpConfig.TypedValue("i", smartSizing ? "1" : "0")));
        return adapted;
    }

    private void saveLocalhostRegistryCache(UUID entry) {
        var ex = WindowsRegistry.local().keyExists(WindowsRegistry.HKEY_CURRENT_USER, "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost");
        if (!ex) {
            return;
        }

        var user = WindowsRegistry.local().readStringValueIfPresent(WindowsRegistry.HKEY_CURRENT_USER,
                "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost", "UsernameHint").orElse(null);
        var cert = WindowsRegistry.local().readBinaryValueIfPresent(WindowsRegistry.HKEY_CURRENT_USER,
                "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost", "CertHash").orElse(null);
        if (user == null && cert == null) {
            return;
        }

        AppCache.update("rdp-" + entry, RegistryCache.builder().usernameHint(user).certHash(cert).build());
    }

    private Optional<RegistryCache> getLocalhostRegistryCache(UUID entry) {
        RegistryCache found = AppCache.getNonNull("rdp-" + entry, RegistryCache.class, () -> null);
        return Optional.ofNullable(found);
    }

    private void prepareLocalhostRegistryCache(RdpLaunchConfig configuration) {
        WindowsRegistry.local().deleteKey(WindowsRegistry.HKEY_CURRENT_USER,
                "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost");

        var localhost = configuration.getConfig().get("full address").orElseThrow().getValue().startsWith("localhost");
        if (localhost) {
            var found = getLocalhostRegistryCache(configuration.getStoreId());
            if (found.isPresent()) {
                var user = found.get().getUsernameHint();
                if (user != null) {
                    WindowsRegistry.local().setStringValue(WindowsRegistry.HKEY_CURRENT_USER,
                            "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost", "UsernameHint", user);
                }

                var cert = found.get().getCertHash();
                if (cert != null) {
                    WindowsRegistry.local().setBinaryValue(WindowsRegistry.HKEY_CURRENT_USER,
                            "Software\\Microsoft\\Terminal Server Client\\Servers\\localhost", "CertHash", cert);
                }
            }
        }
    }

    private String encrypt(SecretValue password) throws Exception {
        var ps = LocalShell.getLocalPowershell().orElseThrow();
        var cmd = ps.command(CommandBuilder.of()
                .add(sc -> "(" + sc.getShellDialect().literalArgument(password.getSecretValue())
                        + " | ConvertTo-SecureString -AsPlainText -Force) | ConvertFrom-SecureString"));
        cmd.sensitive();
        return cmd.readStdoutOrThrow();
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
