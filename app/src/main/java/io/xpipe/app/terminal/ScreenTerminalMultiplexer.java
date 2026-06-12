package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.process.*;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.OsType;
import io.xpipe.app.webtop.WebtopApp;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.Optional;

@Builder
@Jacksonized
@JsonTypeName("screen")
public class ScreenTerminalMultiplexer implements TerminalMultiplexer {

    @Override
    public boolean requiresUnixEnvironment() {
        return true;
    }

    @Override
    public boolean isSupported() throws Exception {
        if (OsType.ofLocal() == OsType.WINDOWS) {
            var p = TerminalProxyManager.getProxy();
            return p.isPresent() && p.get().view().findProgram("screen").isPresent();
        } else {
            return LocalShell.getShell().view().findProgram("screen").isPresent();
        }
    }

    @Override
    public WebtopApp getRequiredWebtopApp() {
        return null;
    }

    @Override
    public boolean supportsSplitView() {
        return false;
    }

    @Override
    public String getDocsLink() {
        return "https://www.gnu.org/software/screen/manual/screen.html";
    }

    @Override
    public boolean shouldSelect() {
        return false;
    }

    @Override
    public void checkSupported(ShellControl sc) throws Exception {
        CommandSupport.isInPathOrThrow(sc, "screen");
    }

    @Override
    public ShellScript launchForExistingSession(ShellControl control, TerminalLaunchConfiguration config) {
        var l = new ArrayList<String>();
        var firstCommand =
                getCommand(control, config.single().getDialectLaunchCommand().buildSimple());
        l.add("screen -S xpipe -X screen -t \"" + escape(config.getCleanTitle(), true) + "\" "
                + escape(firstCommand, false));
        return ShellScript.lines(l);
    }

    @Override
    public ShellScript launchNewSession(ShellControl control, TerminalLaunchConfiguration config) {
        var list = new ArrayList<String>();
        list.add("for scr in $(screen -ls | grep xpipe | awk '{print $1}'); do screen -S $scr -X quit; done");

        var firstCommand =
                getCommand(control, config.single().getDialectLaunchCommand().buildSimple());
        list.add("screen -S xpipe -t \"" + escape(config.getCleanTitle(), true) + "\" " + escape(firstCommand, false));
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
