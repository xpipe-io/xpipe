package io.xpipe.app.terminal;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableFunction;

import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

public class TerminalLauncher {

    public static FilePath constructTerminalInitFile(
            ShellDialect t,
            ShellControl processControl,
            WorkingDirectoryFunction workingDirectory,
            List<String> preInit,
            List<String> postInit,
            TerminalInitScriptConfig config,
            boolean exit)
            throws Exception {
        String nl = t.getNewLine().getNewLineString();
        var content = "";

        var clear = t.clearDisplayCommand();
        if (clear != null && config.isClearScreen()) {
            content += clear + nl;
        }

        // Normalize line endings
        content += nl + preInit.stream().flatMap(s -> s.lines()).collect(Collectors.joining(nl)) + nl;

        // We just apply the profile files always, as we can't be sure that they definitely have been applied.
        // Especially if we launch something that is not the system default shell
        var applyCommand = t.applyInitFileCommand(processControl);
        if (applyCommand != null) {
            content += nl + applyCommand + nl;
        }

        if (config.getDisplayName() != null) {
            content += nl + t.changeTitleCommand(config.getDisplayName()) + nl;
        }

        if (workingDirectory != null && workingDirectory.isSpecified()) {
            var wd = workingDirectory.apply(processControl);
            if (wd != null) {
                content += t.getCdCommand(wd.toString()) + nl;
            }
        }

        // Normalize line endings
        content += nl + postInit.stream().flatMap(s -> s.lines()).collect(Collectors.joining(nl)) + nl;

        if (exit) {
            content += nl + t.getPassthroughExitCommand();
        }

        content = t.prepareScriptContent(content);

        var hash = ScriptHelper.getScriptHash(content);
        var file = t.getInitFileName(processControl, hash);
        return ScriptHelper.createExecScriptRaw(processControl, file, content);
    }

    public static void openDirect(
            String title, FailableFunction<ShellControl, String, Exception> command, ExternalTerminalType type)
            throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var script = constructTerminalInitFile(
                    sc.getShellDialect(),
                    sc,
                    WorkingDirectoryFunction.none(),
                    List.of(),
                    List.of(command.apply(sc)),
                    new TerminalInitScriptConfig(
                            title,
                            type.shouldClear()
                                    && AppPrefs.get().clearTerminalOnInit().get()
                                    && !AppPrefs.get().developerPrintInitFiles().get(),
                            TerminalInitFunction.none()),
                    true);
            var config = new TerminalLaunchConfiguration(null, title, title, true, script, sc.getShellDialect());
            type.launch(config);
        }
    }

    public static void open(String title, ProcessControl cc) throws Exception {
        open(null, title, null, cc, UUID.randomUUID(), true);
    }

    public static void open(String title, ProcessControl cc, UUID request) throws Exception {
        open(null, title, null, cc, request, true);
    }

    public static void open(DataStoreEntry entry, String title, FilePath directory, ProcessControl cc)
            throws Exception {
        open(entry, title, directory, cc, UUID.randomUUID(), true);
    }

    public static void open(
            DataStoreEntry entry, String title, FilePath directory, ProcessControl cc, UUID request, boolean preferTabs)
            throws Exception {
        var type = AppPrefs.get().terminalType().getValue();
        if (type == null) {
            throw ErrorEvent.expected(new IllegalStateException(AppI18n.get("noTerminalSet")));
        }

        var color = entry != null ? DataStorage.get().getEffectiveColor(entry) : null;
        var prefix = entry != null && color != null && type.useColoredTitle() ? color.getEmoji() + " " : "";
        var cleanTitle = (title != null ? title : entry != null ? entry.getName() : "?");
        var adjustedTitle = prefix + cleanTitle;
        var log = AppPrefs.get().enableTerminalLogging().get();
        var terminalConfig = new TerminalInitScriptConfig(
                adjustedTitle,
                !log
                        && type.shouldClear()
                        && AppPrefs.get().clearTerminalOnInit().get()
                        && !AppPrefs.get().developerPrintInitFiles().get(),
                cc instanceof ShellControl ? type.additionalInitCommands() : TerminalInitFunction.none());
        var promptRestart = AppPrefs.get().terminalPromptForRestart().getValue() && AppPrefs.get().terminalMultiplexer().getValue() == null;
        var config = TerminalLaunchConfiguration.create(request, entry, cleanTitle, adjustedTitle, preferTabs, promptRestart);
        var latch = TerminalLauncherManager.submitAsync(request, cc, terminalConfig, directory);
        try {
            if (!checkMultiplexerLaunch(cc, request, config)) {
                if (preferTabs && shouldUseMultiplexer()) {
                    config = config.withPreferTabs(false).withCleanTitle("XPipe").withColoredTitle("XPipe");
                }
                type.launch(config);
            }
            latch.await();
        } catch (Exception ex) {
            var modMsg = ex.getMessage() != null && ex.getMessage().contains("Unable to find application named")
                    ? ex.getMessage() + " in installed /Applications on this system"
                    : ex.getMessage();
            throw ErrorEvent.expected(new IOException(
                    "Unable to launch terminal " + type.toTranslatedString().getValue() + ": " + modMsg, ex));
        }
    }

    private static boolean shouldUseMultiplexer() {
        var type = AppPrefs.get().terminalType().getValue();
        if (type.getOpenFormat() == TerminalOpenFormat.TABBED) {
            return false;
        }

        var multiplexer = AppPrefs.get().terminalMultiplexer().getValue();
        return multiplexer != null;
    }

    private static boolean checkMultiplexerLaunch(ProcessControl processControl, UUID request, TerminalLaunchConfiguration config) throws Exception {
        if (!config.isPreferTabs()) {
            return false;
        }

        if (!shouldUseMultiplexer()) {
            return false;
        }

        if (!TerminalMultiplexerManager.requiresNewTerminalSession(request)) {
            var control = TerminalProxyManager.getProxy();
            if (control.isPresent()) {
                var type = AppPrefs.get().terminalType().getValue();
                var title = type.useColoredTitle() ? config.getColoredTitle() : config.getCleanTitle();
                var openCommand = processControl.prepareTerminalOpen(TerminalInitScriptConfig.ofName(title), WorkingDirectoryFunction.none());
                var multiplexer = AppPrefs.get().terminalMultiplexer().getValue();
                var fullCommand = multiplexer.launchScriptExternal(control.get(), openCommand, TerminalInitScriptConfig.ofName(title)).toString();
                control.get().command(fullCommand).execute();
                return true;
            }
        }
        return false;
    }

    public static String createLaunchCommand(ProcessControl processControl, TerminalInitScriptConfig config, WorkingDirectoryFunction wd) throws Exception {
        var initScript = AppPrefs.get().terminalInitScript().getValue();
        var initialCommand = initScript != null ? initScript.toString() : "";
        var openCommand = processControl.prepareTerminalOpen(config, wd);
        var proxy = TerminalProxyManager.getProxy();
        var multiplexer = AppPrefs.get().terminalMultiplexer().getValue();
        var fullCommand = initialCommand + "\n" + (multiplexer != null ?
                multiplexer.launchScriptSession(proxy.isPresent() ? proxy.get() : LocalShell.getShell(), openCommand, config).toString() : openCommand);
        if (proxy.isPresent()) {
            var proxyOpenCommand = fullCommand;
            var proxyLaunchCommand = proxy.get().prepareIntermediateTerminalOpen(
                    TerminalInitFunction.fixed(proxyOpenCommand),
                    TerminalInitScriptConfig.ofName("XPipe"),
                    WorkingDirectoryFunction.none());
            return proxyLaunchCommand;
        } else {
            return fullCommand;
        }
    }
}
