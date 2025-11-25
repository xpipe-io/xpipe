package io.xpipe.app.terminal;

import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.ScriptHelper;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.process.TerminalInitScriptConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;

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
    public ShellScript launchForExistingSession(ShellControl control, TerminalLaunchConfiguration config) throws Exception {
        // Screen has a limit of 100 chars for commands
//        var effectiveCommand = command.length() > 90
//                ? ScriptHelper.createExecScript(control, command).toString()
//                : command;
//        return ShellScript.lines("screen -S xpipe -X screen -t \"" + escape(config.getDisplayName(), true) + "\" "
//                + escape(effectiveCommand, false));
        return ShellScript.lines();
    }

    @Override
    public ShellScript launchNewSession(ShellControl control, TerminalLaunchConfiguration config) throws Exception {
        var list = new ArrayList<String>();
        list.add("for scr in $(screen -ls | grep xpipe | awk '{print $1}'); do screen -S $scr -X quit; done");
        for (TerminalPaneConfiguration pane : config.getPanes()) {
            var command = pane.getDialectLaunchCommand().buildFull(control);
            // Screen has a limit of 100 chars for commands
            var effectiveCommand = command.length() > 90
                    ? ScriptHelper.createExecScript(control, command).toString()
                    : command;
            list.add("screen -S xpipe -t \"" + escape(pane.getTitle(), true) + "\" "
                    + escape(effectiveCommand, false));
        }
        return ShellScript.lines(list);
    }

    private String escape(String s, boolean quotes) {
        var r = s.replaceAll("\\\\", "\\\\\\\\");
        if (quotes) {
            r = r.replaceAll("\"", "\\\\\"");
        }
        return r;
    }
}
