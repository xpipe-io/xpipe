package io.xpipe.app.rdp;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.RdpConfig;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.SecretValue;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.io.FileUtils;

import java.util.Map;

@JsonTypeName("mstsc")
@Value
@Jacksonized
@Builder
public class MstscRdpClient implements ExternalApplicationType.PathApplication, ExternalRdpClient {

    @SuppressWarnings("unused")
    static OptionsBuilder createOptions(Property<MstscRdpClient> property) {
        var smartSizing = new SimpleObjectProperty<>(property.getValue().isSmartSizing());
        return new OptionsBuilder()
                .nameAndDescription("rdpSmartSizing")
                .addToggle(smartSizing)
                .bind(() -> MstscRdpClient.builder().smartSizing(smartSizing.get()).build(), property);
    }

    boolean smartSizing;

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var adaptedRdpConfig = getAdaptedConfig(configuration);
        var file = writeRdpConfigFile(configuration.getTitle(), adaptedRdpConfig);
        LocalShell.getShell()
                .executeSimpleCommand(CommandBuilder.of().add(getExecutable()).addFile(file.toString()));
        ThreadHelper.runFailableAsync(() -> {
            ThreadHelper.sleep(1000);
            FileUtils.deleteQuietly(file.toFile());
        });
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

    @Override
    public String getId() {
        return "app.mstsc";
    }
}
