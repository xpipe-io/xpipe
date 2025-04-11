package io.xpipe.app.terminal;

import io.xpipe.app.util.CommandSupport;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.process.TerminalInitScriptConfig;

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
    public ShellScript launchScriptExternal(ShellControl control, String command, TerminalInitScriptConfig config)
            throws Exception {
        return ShellScript.lines(
                "zellij attach --create-background xpipe",
                "zellij -s xpipe action new-tab --name \"" + escape(config.getDisplayName(), false, true) + "\"",
                "zellij -s xpipe action write-chars -- " + escape(command, true, true) + "\\;exit",
                "zellij -s xpipe action write 10",
                "zellij -s xpipe action clear"
        );
    }

    @Override
    public ShellScript launchScriptSession(ShellControl control, String command, TerminalInitScriptConfig config)
            throws Exception {
        return ShellScript.lines(
                "zellij delete-session -f xpipe > /dev/null 2>&1",
                "zellij attach --create-background xpipe",
                "zellij -s xpipe run -c --name \"" + escape(config.getDisplayName(), false, true) + "\" -- "
                        + escape(command, false, false),
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
