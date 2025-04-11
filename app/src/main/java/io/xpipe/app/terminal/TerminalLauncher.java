package io.xpipe.app.terminal;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.*;
import io.xpipe.core.store.FilePath;
import io.xpipe.core.util.FailableFunction;
import io.xpipe.core.util.XPipeInstallation;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
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
        var content = constructTerminalInitScript(t, processControl, workingDirectory, preInit, postInit, config, exit);
        var hash = ScriptHelper.getScriptHash(content);
        var file = t.getInitFileName(processControl, hash);
        return ScriptHelper.createExecScriptRaw(processControl, file, content);
    }

    public static String constructTerminalInitScript(
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
        return content;
    }

    public static void openDirect(String title, CommandBuilder command) throws Exception {
        openDirect(
                title,
                sc -> command.buildFull(sc),
                AppPrefs.get().terminalType().getValue());
    }

    public static void openDirect(String title, ShellScript command) throws Exception {
        openDirect(
                title, sc -> command.toString(), AppPrefs.get().terminalType().getValue());
    }

    public static void openDirect(String title, FailableFunction<ShellControl, ShellScript, Exception> command)
            throws Exception {
        openDirect(
                title,
                sc -> command.apply(sc).toString(),
                AppPrefs.get().terminalType().getValue());
    }

    public static void openDirect(
            String title, FailableFunction<ShellControl, String, Exception> command, ExternalTerminalType type)
            throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var script = constructTerminalInitScript(
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
        var promptRestart = AppPrefs.get().terminalPromptForRestart().getValue();
        var latch = TerminalLauncherManager.submitAsync(request, cc, terminalConfig, directory);

        var config = TerminalLaunchConfiguration.create(
                request, entry, cleanTitle, adjustedTitle, preferTabs, promptRestart);

        if (preferTabs && launchMultiplexerTabInExistingTerminal(request, terminalConfig, config)) {
            latch.await();
            return;
        }

        if (preferTabs) {
            var multiplexerConfig = launchMultiplexerTabInNewTerminal(request, terminalConfig, config);
            if (multiplexerConfig.isPresent()) {
                launch(type, multiplexerConfig.get(), latch);
                return;
            }
        }

        var proxyConfig = launchProxy(request, config);
        if (proxyConfig.isPresent()) {
            launch(type, proxyConfig.get(), latch);
            return;
        }

        var initScript = AppPrefs.get().terminalInitScript().getValue();
        var customInit = initScript != null ? initScript + "\n" : "";
        config = config.withScript(
                ProcessControlProvider.get().getEffectiveLocalDialect(),
                getTerminalRegisterCommand(request) + "\n" + customInit + "\n" + config.getScriptContent());
        launch(type, config, latch);
    }

    private static void launch(ExternalTerminalType type, TerminalLaunchConfiguration config, CountDownLatch latch)
            throws Exception {
        try {
            type.launch(config);
            latch.await();
        } catch (Exception ex) {
            var modMsg = ex.getMessage() != null && ex.getMessage().contains("Unable to find application named")
                    ? ex.getMessage() + " in installed /Applications on this system"
                    : ex.getMessage();
            throw ErrorEvent.expected(new IOException(
                    "Unable to launch terminal " + type.toTranslatedString().getValue() + ": " + modMsg, ex));
        }
    }

    private static String getTerminalRegisterCommand(UUID request) throws Exception {
        var exec = XPipeInstallation.getLocalDefaultCliExecutable();
        return CommandBuilder.of()
                .addFile(exec)
                .add("terminal-register", "--request", request.toString())
                .buildFull(LocalShell.getShell());
    }

    private static boolean launchMultiplexerTabInExistingTerminal(
            UUID request, TerminalInitScriptConfig initScriptConfig, TerminalLaunchConfiguration launchConfiguration)
            throws Exception {
        var multiplexer = TerminalMultiplexerManager.getEffectiveMultiplexer();
        if (multiplexer.isEmpty()) {
            return false;
        }

        var control = TerminalProxyManager.getProxy().orElse(LocalShell.getShell());

        // Throw if not supported
        multiplexer.get().checkSupported(control);

        if (TerminalMultiplexerManager.requiresNewTerminalSession(request)) {
            return false;
        }

        var openCommand = launchConfiguration.getDialectLaunchCommand().buildFull(control);
        var multiplexerCommand = multiplexer
                .get()
                .launchScriptExternal(control, openCommand, initScriptConfig)
                .toString();
        control.command(multiplexerCommand).execute();
        return true;
    }

    private static Optional<TerminalLaunchConfiguration> launchMultiplexerTabInNewTerminal(
            UUID request, TerminalInitScriptConfig initScriptConfig, TerminalLaunchConfiguration launchConfiguration)
            throws Exception {
        var multiplexer = TerminalMultiplexerManager.getEffectiveMultiplexer();
        if (multiplexer.isEmpty()) {
            return Optional.empty();
        }

        // Throw if not supported
        multiplexer.get().checkSupported(TerminalProxyManager.getProxy().orElse(LocalShell.getShell()));

        if (!TerminalMultiplexerManager.requiresNewTerminalSession(request)) {
            return Optional.empty();
        }

        var initScript = AppPrefs.get().terminalInitScript().getValue();
        var initialCommand = initScript != null ? initScript + "\n" : "";
        var openCommand = launchConfiguration.getDialectLaunchCommand().buildSimple();
        var fullCommand = initialCommand + openCommand;
        var proxyControl = TerminalProxyManager.getProxy();
        if (proxyControl.isPresent()) {
            var proxyMultiplexerCommand = multiplexer
                    .get()
                    .launchScriptSession(proxyControl.get(), fullCommand, initScriptConfig)
                    .toString();
            var proxyLaunchCommand = proxyControl
                    .get()
                    .prepareIntermediateTerminalOpen(
                            TerminalInitFunction.fixed(proxyMultiplexerCommand),
                            TerminalInitScriptConfig.ofName("XPipe"),
                            WorkingDirectoryFunction.none());
            // Restart for the next time
            proxyControl.get().start();
            var fullLocalCommand = getTerminalRegisterCommand(request) + "\n" + proxyLaunchCommand;
            return Optional.of(new TerminalLaunchConfiguration(
                    null,
                    "XPipe",
                    "XPipe",
                    false,
                    fullLocalCommand,
                    ProcessControlProvider.get().getEffectiveLocalDialect()));
        } else {
            var multiplexerCommand = multiplexer
                    .get()
                    .launchScriptSession(LocalShell.getShell(), fullCommand, initScriptConfig)
                    .toString();
            var launchCommand = LocalShell.getShell()
                    .prepareIntermediateTerminalOpen(
                            TerminalInitFunction.fixed(multiplexerCommand),
                            TerminalInitScriptConfig.ofName("XPipe"),
                            WorkingDirectoryFunction.none());
            var fullLocalCommand = getTerminalRegisterCommand(request) + "\n" + launchCommand;
            return Optional.of(new TerminalLaunchConfiguration(
                    null,
                    "XPipe",
                    "XPipe",
                    false,
                    fullLocalCommand,
                    ProcessControlProvider.get().getEffectiveLocalDialect()));
        }
    }

    private static Optional<TerminalLaunchConfiguration> launchProxy(
            UUID request, TerminalLaunchConfiguration launchConfiguration)
            throws Exception {
        var proxyControl = TerminalProxyManager.getProxy();
        if (proxyControl.isEmpty()) {
            return Optional.empty();
        }

        var initScript = AppPrefs.get().terminalInitScript().getValue();
        var initialCommand = initScript != null ? initScript + "\n" : "";
        var openCommand = launchConfiguration.getDialectLaunchCommand().buildSimple();
        var fullCommand = initialCommand + openCommand;
        var launchCommand = proxyControl
                .get()
                .prepareIntermediateTerminalOpen(
                        TerminalInitFunction.fixed(fullCommand),
                        TerminalInitScriptConfig.ofName("XPipe"),
                        WorkingDirectoryFunction.none());
        // Restart for the next time
        proxyControl.get().start();
        var fullLocalCommand = getTerminalRegisterCommand(request) + "\n" + launchCommand;
        return Optional.ofNullable(launchConfiguration.withScript(
                ProcessControlProvider.get().getEffectiveLocalDialect(), fullLocalCommand));
    }
}
