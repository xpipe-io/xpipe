package io.xpipe.app.terminal;

import io.xpipe.core.process.CommandBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PwshTerminalType extends ExternalTerminalType.SimplePathType implements TrackableTerminalType {

    public PwshTerminalType() {
        super("app.pwsh", "pwsh", true);
    }

    @Override
    public boolean supportsEscapes() {
        return false;
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    public String getWebsite() {
        return "https://learn.microsoft.com/en-us/powershell/scripting/install/installing-powershell?view=powershell-7.4";
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    @Override
    public boolean useColoredTitle() {
        return false;
    }

    @Override
    protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
        return CommandBuilder.of()
                .add("-ExecutionPolicy", "Bypass")
                .add("-EncodedCommand")
                .add(sc -> {
                    // Fix for https://github.com/PowerShell/PowerShell/issues/18530#issuecomment-1325691850
                    var c = "$env:PSModulePath=\"\";"
                            + configuration.getDialectLaunchCommand().buildBase(sc);
                    var base64 = Base64.getEncoder().encodeToString(c.getBytes(StandardCharsets.UTF_16LE));
                    return "\"" + base64 + "\"";
                });
    }
}
