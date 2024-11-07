package io.xpipe.app.terminal;

import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PowerShellTerminalType extends ExternalTerminalType.SimplePathType implements DockableTerminalType {

    public PowerShellTerminalType() {
        super("app.powershell", "powershell", true);
    }

    @Override
    public boolean supportsTabs() {
        return false;
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    @Override
    public boolean supportsColoredTitle() {
        return false;
    }

    @Override
    protected CommandBuilder toCommand(LaunchConfiguration configuration) {
        if (configuration.getScriptDialect().equals(ShellDialects.POWERSHELL)) {
            return CommandBuilder.of()
                    .add("-ExecutionPolicy", "Bypass")
                    .add("-File")
                    .addFile(configuration.getScriptFile());
        }

        return CommandBuilder.of()
                .add("-ExecutionPolicy", "Bypass")
                .add("-EncodedCommand")
                .add(sc -> {
                    var base64 = Base64.getEncoder()
                            .encodeToString(configuration
                                    .getDialectLaunchCommand()
                                    .buildBase(sc)
                                    .getBytes(StandardCharsets.UTF_16LE));
                    return "\"" + base64 + "\"";
                });
    }
}
