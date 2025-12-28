package io.xpipe.app.terminal;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

public class PwshTerminalType implements ExternalApplicationType.PathApplication, TrackableTerminalType {

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
    public boolean supportsEscapes() {
        return false;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        var b = CommandBuilder.of()
                .add("-ExecutionPolicy", "Bypass")
                .add("-EncodedCommand")
                .add(sc -> {
                    // Fix for https://github.com/PowerShell/PowerShell/issues/18530#issuecomment-1325691850
                    var c = "$env:PSModulePath=\"\";"
                            + configuration.single().getDialectLaunchCommand().buildBase(sc);
                    var base64 = Base64.getEncoder().encodeToString(c.getBytes(StandardCharsets.UTF_16LE));
                    return "\"" + base64 + "\"";
                });
        launch(b);
    }

    @Override
    public String getExecutable() {
        return "pwsh.exe";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getId() {
        return "app.pwsh";
    }
}
