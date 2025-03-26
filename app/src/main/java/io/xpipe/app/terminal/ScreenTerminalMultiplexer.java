package io.xpipe.app.terminal;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.process.TerminalInitScriptConfig;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@JsonTypeName("screen")
public class ScreenTerminalMultiplexer implements TerminalMultiplexer {

    @Override
    public String getDocsLink() {
        return "https://github.com/tmux/tmux/wiki/Getting-Started";
    }

    @Override
    public void checkSupported(ShellControl sc) throws Exception {
        CommandSupport.isInPathOrThrow(sc, "screen");
    }

    @Override
    public ShellScript launchScriptExternal(ShellControl control, String command, TerminalInitScriptConfig config) throws Exception {
        return ShellScript.lines(
                "screen -S xpipe -X screen -t \"" + escape(config.getDisplayName(), true) + "\" " + escape(command, false)
        );
    }

    @Override
    public ShellScript launchScriptSession(ShellControl control, String command, TerminalInitScriptConfig config) throws Exception {
        return ShellScript.lines(
                "for scr in $(screen -ls | grep xpipe | awk '{print $1}'); do screen -S $scr -X quit; done",
                "screen -S xpipe -t \"" + escape(config.getDisplayName(), true) + "\" " + escape(command, false)
        );
    }

    private String escape(String s, boolean quotes) {
        var r = s.replaceAll("\\\\", "\\\\\\\\");
        if (quotes) {
            r = r.replaceAll("\"", "\\\\\"");
        }
        return r;
    }
}
