package io.xpipe.app.terminal;

import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.process.TerminalInitScriptConfig;
import io.xpipe.app.process.CommandSupport;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@JsonTypeName("zellij")
public class ZellijTerminalMultiplexer implements TerminalMultiplexer {

    @Override
    public String getDocsLink() {
        return "https://zellij.dev/";
    }

    @Override
    public void checkSupported(ShellControl sc) throws Exception {
        CommandSupport.isInPathOrThrow(sc, "zellij");
    }

    @Override
    public ShellScript launchForExistingSession(ShellControl control, String command, TerminalInitScriptConfig config) {
        return ShellScript.lines(
                "zellij attach --create-background xpipe",
                "zellij -s xpipe action new-tab --name \"" + escape(config.getDisplayName(), false, true) + "\"",
                "zellij -s xpipe action write-chars -- " + escape(" " + command, true, true) + "\\;exit",
                "zellij -s xpipe action write 10",
                "zellij -s xpipe action clear");
    }

    @Override
    public ShellScript launchNewSession(ShellControl control, String command, TerminalInitScriptConfig config) {
        return ShellScript.lines(
                "zellij delete-session -f xpipe > /dev/null 2>&1",
                "zellij attach --create-background xpipe",
                "sleep 0.5",
                "zellij -s xpipe run -c --name \"" + escape(config.getDisplayName(), false, true) + "\" -- "
                        + escape(" " + command, false, false),
                "sleep 0.5",
                "zellij attach xpipe");
    }

    private String escape(String s, boolean spaces, boolean quotes) {
        var r = s.replaceAll("\\\\", "\\\\\\\\");
        if (quotes) {
            r = r.replaceAll("\"", "\\\\\"");
        }
        if (spaces) {
            r = r.replaceAll(" ", "\\\\ ");
        }
        return r;
    }
}
