package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.process.TerminalInitScriptConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

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
    public ShellScript launchForExistingSession(ShellControl control, TerminalLaunchConfiguration config) throws Exception {
        var l = new ArrayList<String>();
        var firstCommand = config.getPanes().getFirst().getDialectLaunchCommand().buildFull(control);
        l.addAll(List.of(
                "tmux new-window -t xpipe -n \"" + escape(config.getColoredTitle(), true) + "\" "
                        + escape(firstCommand, false)
        ));
        for (int i = 1; i < config.getPanes().size(); i++) {
            var iCommand = config.getPanes().get(i).getDialectLaunchCommand().buildFull(control);
            l.add("tmux split-window -t xpipe " + escape(iCommand, false));
        }
        return ShellScript.lines(l);
    }

    @Override
    public ShellScript launchNewSession(ShellControl control, TerminalLaunchConfiguration config) throws Exception {
        var l = new ArrayList<String>();
        var firstCommand = config.getPanes().getFirst().getDialectLaunchCommand().buildFull(control);
        l.addAll(List.of(
                "tmux kill-session -t xpipe >/dev/null 2>&1",
                "tmux new-session -d -s xpipe",
                "tmux rename-window \"" + escape(config.getColoredTitle(), true) + "\"",
                "tmux send-keys -t xpipe ' clear; " + escape(firstCommand, false) + "; exit' Enter"
        ));

        if (config.getPanes().size() > 1) {
            for (int i = 1; i < config.getPanes().size(); i++) {
                var iCommand = config.getPanes().get(i).getDialectLaunchCommand().buildFull(control);
                l.add("tmux split-window -t xpipe " + escape(iCommand, false));
            }

            var splitStrategy = AppPrefs.get().terminalSplitStrategy().getValue();
            var layoutName = splitStrategy == TerminalSplitStrategy.HORIZONTAL ? "even-horizontal" :
                    splitStrategy == TerminalSplitStrategy.VERTICAL ? "even-vertical" : "tiled";
            l.add("tmux select-layout -t xpipe " + layoutName);
        }

        l.add("tmux attach -d -t xpipe");

        return ShellScript.lines(l);
    }

    private String escape(String s, boolean quotes) {
        var r = s.replaceAll("\\\\", "\\\\\\\\");
        if (quotes) {
            r = r.replaceAll("\"", "\\\\\"");
        }
        return r;
    }
}
