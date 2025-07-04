package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.RdpConfig;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.core.SecretValue;

import org.apache.commons.io.FileUtils;

import java.util.Map;

public class MstscRdpClient implements ExternalApplicationType.PathApplication, ExternalRdpClient {

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
        return true;
    }

    private RdpConfig getAdaptedConfig(RdpLaunchConfig configuration) throws Exception {
        var input = configuration.getConfig();
        if (input.get("password 51").isPresent()) {
            return input;
        }

        if (input.get("username").isEmpty()) {
            // return input;
        }

        var pass = configuration.getPassword();
        if (pass == null) {
            return input;
        }

        var adapted = input.overlay(Map.of(
                "password 51",
                new RdpConfig.TypedValue("b", encrypt(pass)),
                "prompt for credentials",
                new RdpConfig.TypedValue("i", "0")));
        return adapted;
    }

    private String encrypt(SecretValue password) throws Exception {
        var ps = LocalShell.getLocalPowershell();
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
