package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.*;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.GlobalTimer;
import io.xpipe.app.util.ThreadHelper;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Builder
@Jacksonized
@JsonTypeName("zellij")
public class ZellijTerminalMultiplexer implements TerminalMultiplexer {

    @Override
    public boolean supportsSplitView() {
        return true;
    }

    @Override
    public String getDocsLink() {
        return "https://zellij.dev/";
    }

    @Override
    public void checkSupported(ShellControl sc) throws Exception {
        CommandSupport.isInPathOrThrow(sc, "zellij");
    }

    @Override
    public ShellScript launchForExistingSession(ShellControl control, TerminalLaunchConfiguration config)
            throws Exception {
        var l = new ArrayList<String>();
        var firstCommand =
                config.getPanes().getFirst().getDialectLaunchCommand().buildFull(control);
        l.addAll(List.of(
                "zellij attach --create-background xpipe",
                "zellij -s xpipe action new-tab --name \"" + escape(config.getColoredTitle(), false, true) + "\"",
                "zellij -s xpipe action write-chars -- " + escape(" " + firstCommand, true, true) + "\\;exit",
                "zellij -s xpipe action write 10",
                "zellij -s xpipe action clear"));

        var split = AppPrefs.get().terminalSplitStrategy().getValue();
        for (int i = 1; i < config.getPanes().size(); i++) {
            var iCommand = config.getPanes().get(i).getDialectLaunchCommand().buildFull(control);
            var direction = split == TerminalSplitStrategy.HORIZONTAL
                    ? "--direction right "
                    : split == TerminalSplitStrategy.VERTICAL ? "--direction down " : "";
            l.addAll(List.of(
                    "zellij -s xpipe action new-pane " + direction + "--name \""
                            + escape(config.getPanes().get(i).getTitle(), false, true) + "\"",
                    "zellij -s xpipe action write-chars -- " + escape(" " + iCommand, true, true) + "\\;exit",
                    "zellij -s xpipe action write 10",
                    "zellij -s xpipe action clear"));
        }

        return ShellScript.lines(l);
    }

    @Override
    public ShellScript launchNewSession(ShellControl control, TerminalLaunchConfiguration config) throws Exception {
        var l = new ArrayList<String>();
        var firstConfig = config.getPanes().getFirst();
        var firstCommand = firstConfig.getDialectLaunchCommand().buildFull(control);
        l.addAll(List.of(
                "zellij delete-session -f xpipe > /dev/null 2>&1",
                "zellij attach --create xpipe"));

        var asyncLines = new ArrayList<String>();
       asyncLines.addAll(List.of(
               "sleep 0.5",
                "zellij -s xpipe action new-tab --name \"" + escape(config.getColoredTitle(), false, true) + "\"",
                "zellij -s xpipe action write-chars -- " + escape(" " + firstCommand, true, true) + "\\;exit",
                "zellij -s xpipe action write 10",
                "zellij -s xpipe action clear",
                "zellij -s xpipe action rename-tab \"" + escape(config.getColoredTitle(), false, true) + "\"",
               "sleep 0.5",
                "zellij -s xpipe action go-to-previous-tab",
                "zellij -s xpipe action close-tab"));

        if (config.getPanes().size() > 1) {
            var split = AppPrefs.get().terminalSplitStrategy().getValue();
            for (int i = 1; i < config.getPanes().size(); i++) {
                var iCommand =
                        config.getPanes().get(i).getDialectLaunchCommand().buildFull(control);
                var direction = split == TerminalSplitStrategy.HORIZONTAL
                        ? "--direction right "
                        : split == TerminalSplitStrategy.VERTICAL ? "--direction down " : "";
                asyncLines.addAll(List.of(
                        "sleep 0.5",
                        "zellij -s xpipe action new-pane " + direction + "--name \""
                                + escape(config.getPanes().get(i).getTitle(), false, true) + "\"",
                        "zellij -s xpipe action write-chars -- " + escape(" " + iCommand, true, true) + "\\;exit",
                        "zellij -s xpipe action write 10",
                        "zellij -s xpipe action clear"));
            }
        }

        var listener = new TerminalView.Listener() {
            @Override
            @SneakyThrows
            public void onSessionOpened(TerminalView.ShellSession session) {
                TerminalView.get().removeListener(this);
                ThreadHelper.runFailableAsync(() -> {
                    var sc = TerminalProxyManager.getProxy().orElse(LocalShell.getShell());
                    sc.command(String.join("\n", asyncLines)).executeAndCheck();
                });
            }
        };
        TerminalView.get().addListener(listener);

        return ShellScript.lines(l);
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
