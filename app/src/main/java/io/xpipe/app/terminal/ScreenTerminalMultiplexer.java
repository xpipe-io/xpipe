package io.xpipe.app.terminal;

import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.ScriptHelper;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Builder
@Jacksonized
@JsonTypeName("screen")
public class ScreenTerminalMultiplexer implements TerminalMultiplexer {

    @Override
    public boolean supportsSplitView() {
        return false;
    }

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
        var l = new ArrayList<String>();
        var firstCommand = getCommand(control, config.single().getDialectLaunchCommand().buildFull(control));
        l.addAll(List.of(
                "screen -S xpipe -X screen -t \"" + escape(config.getCleanTitle(), true) + "\" "
                        + escape(firstCommand, false)
        ));
        return ShellScript.lines(l);
    }

    @Override
    public ShellScript launchNewSession(ShellControl control, TerminalLaunchConfiguration config) throws Exception {
        var list = new ArrayList<String>();
        list.add("for scr in $(screen -ls | grep xpipe | awk '{print $1}'); do screen -S $scr -X quit; done");

        var firstCommand = getCommand(control, config.single().getDialectLaunchCommand().buildFull(control));
        list.addAll(List.of(
                "screen -S xpipe -t \"" + escape(config.getCleanTitle(), true) + "\" "
                        + escape(firstCommand, false)
        ));
        return ShellScript.lines(list);
    }

    private String getCommand(ShellControl sc, String command) {
        // Screen has a limit of 100 chars for commands
        var effectiveCommand = command.length() > 90
                ? ScriptHelper.createExecScript(sc, command).toString()
                : command;
        return effectiveCommand;
    }

    private String escape(String s, boolean quotes) {
        var r = s.replaceAll("\\\\", "\\\\\\\\");
        if (quotes) {
            r = r.replaceAll("\"", "\\\\\"");
        }
        return r;
    }
}
