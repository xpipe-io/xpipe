package io.xpipe.app.terminal;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.*;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.FilePath;
import io.xpipe.app.util.OsType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.webtop.WebtopApp;
import lombok.Builder;
import lombok.SneakyThrows;
import lombok.extern.jackson.Jacksonized;

import java.util.ArrayList;
import java.util.List;

@Builder
@Jacksonized
@JsonTypeName("zellij")
public class ZellijTerminalMultiplexer implements TerminalMultiplexer {

    @Override
    public boolean requiresUnixEnvironment() {
        // TODO: It seems like zellij is still broken on Windows
        // Check back later
        return true;
    }

    @Override
    public boolean isSupported() throws Exception {
        if (OsType.ofLocal() == OsType.WINDOWS) {
            var p = TerminalProxyManager.getProxy();
            if (p.isPresent() && p.get().view().findProgram("zellij").isPresent()) {
                return true;
            }
        }
        return LocalShell.getShell().view().findProgram("zellij").isPresent();
    }

    @Override
    public WebtopApp getRequiredWebtopApp() {
        return WebtopApp.ZELLIJ;
    }

    @Override
    public boolean supportsSplitView() {
        return true;
    }

    @Override
    public String getDocsLink() {
        return "https://zellij.dev/";
    }

    @Override
    public boolean shouldSelect() throws Exception {
        return LocalShell.getShell().view().findProgram("zellij").isPresent();
    }

    @Override
    public void checkSupported(ShellControl sc) throws Exception {
        CommandSupport.isInPathOrThrow(sc, "zellij");
    }

    @Override
    public ShellScript launchForExistingSession(ShellControl control, TerminalLaunchConfiguration config) {
        var l = new ArrayList<String>();
        var firstCommand =
                config.getPanes().getFirst().getDialectLaunchCommand().buildSimple();
        l.addAll(List.of(
                "zellij attach --create-background xpipe",
                "zellij -s xpipe action new-tab --name \"" + escape(control, config.getColoredTitle(), false, true) + "\"",
                "zellij -s xpipe action write-chars -- " + escape(control, " " + firstCommand, true, true) + getCommandExitLiteral(control),
                "zellij -s xpipe action clear",
                "zellij -s xpipe action write 10"));

        if (config.getPanes().size() > 1) {
            var splitIterator =
                    AppPrefs.get().terminalSplitStrategy().getValue().iterator();
            splitIterator.next();

            for (int i = 1; i < config.getPanes().size(); i++) {
                var iCommand =
                        config.getPanes().get(i).getDialectLaunchCommand().buildSimple();
                var direction = splitIterator.getSplitDirection();
                var directionString = direction == TerminalSplitStrategy.SplitDirection.HORIZONTAL
                        ? "--direction right"
                        : "--direction down";
                l.addAll(List.of(
                        "zellij -s xpipe action new-pane " + directionString
                                + " --name \""
                                + escape(control, config.getPanes().get(i).getTitle(), false, true)
                                + "\"",
                        "zellij -s xpipe action write-chars -- " + escape(control, " " + iCommand, true, true) + getCommandExitLiteral(control),
                        "zellij -s xpipe action clear",
                        "zellij -s xpipe action write 10",
                        "zellij -s xpipe action focus-next-pane"));
                splitIterator.next();
            }
        }

        return ShellScript.lines(l);
    }

    @Override
    public ShellScript launchNewSession(ShellControl control, TerminalLaunchConfiguration config) throws Exception {
        var configFile = getConfigFile(control);

        if (!control.view().fileExists(configFile)) {
            var def = control.command(CommandBuilder.of().add("zellij", "setup", "--dump-config"))
                    .readStdoutOrThrow();
            control.view().mkdir(configFile.getParent());
            control.view().writeTextFile(configFile, def);
        }

        var configText = control.view().readTextFile(configFile);
        var comment = "// show_startup_tips false";
        if (configText.contains(comment)) {
            var replaced = configText.replace(comment, "show_startup_tips false");
            control.view().writeTextFile(configFile, replaced);
        } else if (!configText.contains("show_startup_tips")) {
            control.view().writeTextFile(configFile, configText + "\n" + "show_startup_tips false");
        }

        var firstConfig = config.getPanes().getFirst();
        var firstCommand = firstConfig.getDialectLaunchCommand().buildSimple();

        // Set proper env variables for a terminal
        try (var sub = control.identicalDialectSubShell().start()) {
            sub.writeLine(sub.getShellDialect().prepareTerminalEnvironmentCommands(true));
            sub.command("zellij delete-session -f xpipe").executeAndCheck();
            sub.command("zellij attach --create-background xpipe").executeAndCheck();
        }

        var asyncLines = new ArrayList<String>();
        asyncLines.addAll(List.of(
                "sleep 0.5",
                "zellij -s xpipe action new-tab --name \"" + escape(control, config.getColoredTitle(), false, true) + "\"",
                "sleep 0.5",
                "zellij -s xpipe action go-to-tab 2",
                "sleep 0.5",
                "zellij -s xpipe action write-chars -- " + escape(control, " " + firstCommand, true, true) + getCommandExitLiteral(control),
                "zellij -s xpipe action clear",
                "zellij -s xpipe action write 10",
                "zellij -s xpipe action go-to-tab 1",
                "zellij -s xpipe action close-tab"));

        if (config.getPanes().size() > 1) {
            var splitIterator =
                    AppPrefs.get().terminalSplitStrategy().getValue().iterator();
            splitIterator.next();
            for (int i = 1; i < config.getPanes().size(); i++) {
                var iCommand =
                        config.getPanes().get(i).getDialectLaunchCommand().buildSimple();
                var direction = splitIterator.getSplitDirection();
                var directionString = direction == TerminalSplitStrategy.SplitDirection.HORIZONTAL
                        ? "--direction right"
                        : "--direction down";
                asyncLines.addAll(List.of(
                        "zellij -s xpipe action new-pane " + directionString + " --name \""
                                + escape(control, config.getPanes().get(i).getTitle(), false, true) + "\"",
                        "zellij -s xpipe action write-chars -- " + escape(control, " " + iCommand, true, true) + getCommandExitLiteral(control),
                        "zellij -s xpipe action clear",
                        "zellij -s xpipe action write 10",
                        "zellij -s xpipe action focus-next-pane"));
                splitIterator.next();
            }
        }

        var listener = new TerminalView.Listener() {
            @Override
            @SneakyThrows
            public void onSessionOpened(TerminalView.ShellSession session) {
                TerminalView.get().removeListener(this);

                // We might have changed the prefs meanwhile
                var isActive = TerminalMultiplexerManager.getEffectiveMultiplexer()
                        .map(terminalMultiplexer -> terminalMultiplexer == ZellijTerminalMultiplexer.this).orElse(false);
                if (!isActive) {
                    return;
                }

                ThreadHelper.runFailableAsync(() -> {
                    var command = control.command(String.join("\n", asyncLines));
                    // Zellij sometimes freezes
                    command.killOnTimeout(CountDown.of().start(10_000));
                    command.executeAndCheck();
                });
            }
        };
        TerminalView.get().addListener(listener);

        var l = new ArrayList<String>();
        l.add("zellij attach xpipe");
        l.add("Start-Sleep -s 50");
        return ShellScript.lines(l);
    }

    private String getCommandExitLiteral(ShellControl sc) {
        return sc.getOsType() == OsType.WINDOWS ? "`;exit" : "\\;exit";
    }

    private FilePath getConfigFile(ShellControl sc) throws Exception {
        if (sc.getOsType() == OsType.MACOS) {
            return sc.view()
                    .userHome()
                    .join("Library", "Application Support", "org.Zellij-Contributors.Zellij", "config.kdl");
        } else if (sc.getOsType() == OsType.WINDOWS) {
            return sc.view()
                    .userHome()
                    .join(".config", "zellij", "config.kdl");
        } else {
            return sc.view()
                    .getEnvironmentVariable("XDG_HOME")
                    .map(FilePath::of)
                    .orElse(sc.view().userHome().join(".config"))
                    .join("zellij", "config.kdl");
        }
    }

    private String escape(ShellControl sc, String s, boolean spaces, boolean quotes) {
        var escapeString = sc.getOsType() == OsType.WINDOWS ? "`" : "\\\\";
        var r = s.replaceAll(escapeString, escapeString + escapeString);
        if (quotes) {
            r = r.replaceAll("\"", escapeString + "'");
        }
        if (spaces) {
            r = r.replaceAll(" ", escapeString + " ");
        }
        return r;
    }
}
