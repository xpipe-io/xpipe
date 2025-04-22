package io.xpipe.app.terminal;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PowerShellTerminalType extends ExternalTerminalType.SimplePathType implements TrackableTerminalType {

    @Override
    public boolean supportsEscapes() {
        return false;
    }

    public PowerShellTerminalType() {
        super("app.powershell", "powershell", true);
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    public int getProcessHierarchyOffset() {
        var powershell = ShellDialects.isPowershell(ProcessControlProvider.get().getEffectiveLocalDialect());
        return powershell ? -1 : 0;
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
        if (configuration.getScriptDialect().equals(ShellDialects.POWERSHELL)) {
            return CommandBuilder.of()
                    .add("-ExecutionPolicy", "Bypass")
                    .add("-File")
                    .addQuoted(configuration.getScriptFile().toString());
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
