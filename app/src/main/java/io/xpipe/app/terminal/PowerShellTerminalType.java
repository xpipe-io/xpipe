package io.xpipe.app.terminal;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.ShellDialects;
import io.xpipe.app.util.LocalShell;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PowerShellTerminalType implements ExternalApplicationType.PathApplication, TrackableTerminalType {

    @Override
    public boolean supportsEscapes() {
        return false;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        launch(toCommand(configuration));
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW;
    }

    @Override
    public int getProcessHierarchyOffset() {
        var powershell = ShellDialects.isPowershell(LocalShell.getDialect());
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

    protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
        if (configuration.getScriptDialect() == ShellDialects.POWERSHELL) {
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

    @Override
    public String getExecutable() {
        return "powershell.exe";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getId() {
        return "app.powershell";
    }
}
