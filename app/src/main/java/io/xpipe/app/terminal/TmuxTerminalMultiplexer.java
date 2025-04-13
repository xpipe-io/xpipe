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
@JsonTypeName("tmux")
public class TmuxTerminalMultiplexer implements TerminalMultiplexer {

    @Override
    public String getDocsLink() {
        return "https://github.com/tmux/tmux/wiki/Getting-Started";
    }

    @Override
    public void checkSupported(ShellControl sc) throws Exception {
        CommandSupport.isInPathOrThrow(sc, "tmux");
    }

    @Override
    public ShellScript launchScriptExternal(ShellControl control, String command, TerminalInitScriptConfig config) {
        return ShellScript.lines("tmux new-window -t xpipe -n \"" + escape(config.getDisplayName(), true) + "\" "
                + escape(command, false));
    }

    @Override
    public ShellScript launchScriptSession(ShellControl control, String command, TerminalInitScriptConfig config) {
        return ShellScript.lines(
                "tmux kill-session -t xpipe",
                "tmux new-session -d -s xpipe",
                "tmux rename-window \"" + escape(config.getDisplayName(), true) + "\"",
                "tmux send-keys -t xpipe ' " + escape(command, false) + "; exit' Enter",
                "tmux attach -d -t xpipe");
    }

    private String escape(String s, boolean quotes) {
        var r = s.replaceAll("\\\\", "\\\\\\\\");
        if (quotes) {
            r = r.replaceAll("\"", "\\\\\"");
        }
        return r;
    }
}
