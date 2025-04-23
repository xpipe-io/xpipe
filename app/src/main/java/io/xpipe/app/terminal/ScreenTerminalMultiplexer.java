package io.xpipe.app.terminal;

import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.process.TerminalInitScriptConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@JsonTypeName("screen")
public class ScreenTerminalMultiplexer implements TerminalMultiplexer {

    @Override
    public String getDocsLink() {
        return "https://www.gnu.org/software/screen/manual/screen.html";
    }

    @Override
    public void checkSupported(ShellControl sc) throws Exception {
        CommandSupport.isInPathOrThrow(sc, "screen");
    }

    @Override
    public ShellScript launchForExistingSession(ShellControl control, String command, TerminalInitScriptConfig config) {
        // Screen has a limit of 100 chars for commands
        var effectiveCommand = command.length() > 90
                ? ScriptHelper.createExecScript(control, command).toString()
                : command;
        return ShellScript.lines("screen -S xpipe -X screen -t \"" + escape(config.getDisplayName(), true) + "\" "
                + escape(effectiveCommand, false));
    }

    @Override
    public ShellScript launchNewSession(ShellControl control, String command, TerminalInitScriptConfig config) {
        // Screen has a limit of 100 chars for commands
        var effectiveCommand = command.length() > 90
                ? ScriptHelper.createExecScript(control, command).toString()
                : command;
        return ShellScript.lines(
                "for scr in $(screen -ls | grep xpipe | awk '{print $1}'); do screen -S $scr -X quit; done",
                "screen -S xpipe -t \"" + escape(config.getDisplayName(), true) + "\" "
                        + escape(effectiveCommand, false));
    }

    private String escape(String s, boolean quotes) {
        var r = s.replaceAll("\\\\", "\\\\\\\\");
        if (quotes) {
            r = r.replaceAll("\"", "\\\\\"");
        }
        return r;
    }
}
