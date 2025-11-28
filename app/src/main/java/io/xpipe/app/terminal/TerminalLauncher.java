package io.xpipe.app.terminal;

import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppNames;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.process.*;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.core.FailableFunction;
import io.xpipe.core.FilePath;
import io.xpipe.core.OsType;
import lombok.Value;

import java.io.IOException;
import java.util.ArrayList;
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

    private static String constructTerminalInitScript(
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

        content = t.prepareScriptContent(processControl, content);
        return content;
    }

    static void openDirect(
            String title, FailableFunction<ShellControl, ShellScript, Exception> command, ExternalTerminalType type)
            throws Exception {
        try (var sc = LocalShell.getShell().start()) {
            var script = constructTerminalInitScript(
                    sc.getShellDialect(),
                    sc,
                    WorkingDirectoryFunction.none(),
                    List.of(),
                    List.of(command.apply(sc).toString()),
                    new TerminalInitScriptConfig(
                            title,
                            type.shouldClear()
                                    && AppPrefs.get().clearTerminalOnInit().get()
                                    && !AppPrefs.get().developerPrintInitFiles().get(),
                            TerminalInitFunction.none()),
                    true);
            var singlePane = new TerminalPaneConfiguration(UUID.randomUUID(), title, 0, script, sc.getShellDialect());
            var config = new TerminalLaunchConfiguration(null, title, title, true, List.of(singlePane));
            launch(type, config, new CountDownLatch(0));
        }
    }

    @Value
    public static class Config {
        DataStoreEntry entry;
        String title;
        FilePath directory;
        UUID request;
        boolean enableLogging;
        boolean alwaysKeepOpen;
        ProcessControl processControl;
    }

    public static void open(
            List<Config> configs,
            boolean preferTabs,
            ExternalTerminalType type)
            throws Exception {

        var latch = new CountDownLatch(configs.size());
        var paneList = new ArrayList<TerminalPaneConfiguration>();
        for (Config config : configs) {
            var entry = config.getEntry();
            var color = entry != null ? DataStorage.get().getEffectiveColor(entry) : null;
            var prefix = entry != null && color != null && type.useColoredTitle() ? color.getEmoji() + " " : "";
            var cleanTitle = (config.getTitle() != null ? config.getTitle() : entry != null ? entry.getName() : "Unknown");
            var adjustedTitle = prefix + cleanTitle;

            var log = config.isEnableLogging() && AppPrefs.get().enableTerminalLogging().get();
            var terminalConfig = new TerminalInitScriptConfig(adjustedTitle,
                    !log && type.shouldClear() && AppPrefs.get().clearTerminalOnInit().get() && !AppPrefs.get().developerPrintInitFiles().get(),
                    config.getProcessControl() instanceof ShellControl ? type.additionalInitCommands() : TerminalInitFunction.none());
            var alwaysPromptRestart = config.isAlwaysKeepOpen() || AppPrefs.get().terminalAlwaysPauseOnExit().getValue();
            TerminalLauncherManager.submitAsync(config.getRequest(), config.getProcessControl(), terminalConfig, config.getDirectory(), latch);
            var effectivePreferTabs = preferTabs && AppPrefs.get().preferTerminalTabs().get();

            var paneIndex = configs.indexOf(config);
            var paneConfig = TerminalPaneConfiguration.create(config.getRequest(),
                    entry, config.getTitle(), paneIndex, effectivePreferTabs,
                    alwaysPromptRestart);
            paneList.add(paneConfig);
        }

        var title = configs.size() == 1 ? configs.getFirst().getTitle() : AppNames.ofCurrent().getName() + " (" + configs.size() + " tabs)";
        var entry = configs.size() == 1 ? configs.getFirst().getEntry() : null;
        var color = entry != null ? DataStorage.get().getEffectiveColor(entry) : null;
        var prefix = entry != null && color != null && type.useColoredTitle() ? color.getEmoji() + " " : "";
        var cleanTitle = (title != null ? title : entry != null ? entry.getName() : "Unknown");
        var adjustedTitle = prefix + cleanTitle;

        var effectivePreferTabs =
                preferTabs && AppPrefs.get().preferTerminalTabs().get();
        var launchConfig = new TerminalLaunchConfiguration(color, adjustedTitle, cleanTitle, preferTabs, paneList);

        // Used for multiplexers and proxies
        var launchRequest = UUID.randomUUID();

        if (effectivePreferTabs) {
            synchronized (TerminalLauncher.class) {
                // There will be timing issues when launching multiple tabs in a short time span
                TerminalMultiplexerManager.synchronizeMultiplexerLaunchTiming();

                // Let multiplexer know we launched something, even if there is no multiplexer
                // This is to be prepared for settings changes later on where the multiplexer used is changed
                TerminalMultiplexerManager.registerSessionLaunch(launchRequest, launchConfig);

                if (launchMultiplexerTabInExistingTerminal(launchConfig)) {
                    latch.await();
                    return;
                }

                var multiplexerConfig = launchMultiplexerTabInNewTerminal(launchConfig, launchRequest);
                if (multiplexerConfig.isPresent()) {
                    // Use first tab to track when multiplexer has started up
                    TerminalMultiplexerManager.registerMultiplexerLaunch(paneList.getFirst().getRequest());
                    launch(type, multiplexerConfig.get(), latch);
                    return;
                }
            }
        }

        var proxyConfig = launchProxy(launchConfig, launchRequest);
        if (proxyConfig.isPresent()) {
            TerminalProxyManager.registerSessionLaunch(launchRequest, launchConfig);
            launch(type, proxyConfig.get(), latch);
            return;
        }

        launch(type, launchConfig, latch);
    }

    private static void launch(ExternalTerminalType type, TerminalLaunchConfiguration config, CountDownLatch latch)
            throws Exception {
        if (type == null) {
            return;
        }

        try {
            type.launch(config);
            latch.await();
        } catch (Exception ex) {
            var modMsg = ex.getMessage() != null && ex.getMessage().contains("Unable to find application named")
                    ? ex.getMessage() + " in installed /Applications on this system"
                    : ex.getMessage();
            throw ErrorEventFactory.expected(new IOException(
                    "Unable to launch terminal " + type.toTranslatedString().getValue() + ": " + modMsg, ex));
        }
    }

    private static boolean launchMultiplexerTabInExistingTerminal(TerminalLaunchConfiguration launchConfiguration)
            throws Exception {
        var multiplexer = TerminalMultiplexerManager.getEffectiveMultiplexer();
        if (multiplexer.isEmpty()) {
            return false;
        }

        var control = TerminalProxyManager.getProxy().orElse(LocalShell.getShell());

        // Throw if not supported
        multiplexer.get().checkSupported(control);

        var session = TerminalMultiplexerManager.getActiveMultiplexerSession();
        if (session.isEmpty()) {
            return false;
        }

        var multiplexerCommand = multiplexer
                .get()
                .launchForExistingSession(control, launchConfiguration)
                .toString();
        control.command(multiplexerCommand).execute();
        TerminalView.focus(session.get());
        return true;
    }

    private static Optional<TerminalLaunchConfiguration> launchMultiplexerTabInNewTerminal(
            TerminalLaunchConfiguration launchConfiguration, UUID launchRequest)
            throws Exception {
        var multiplexer = TerminalMultiplexerManager.getEffectiveMultiplexer();
        if (multiplexer.isEmpty()) {
            return Optional.empty();
        }

        // Throw if not supported
        multiplexer.get().checkSupported(TerminalProxyManager.getProxy().orElse(LocalShell.getShell()));

        if (TerminalMultiplexerManager.getActiveMultiplexerSession().isPresent()) {
            return Optional.empty();
        }

        var multiplexerLaunchRequest = UUID.randomUUID();

        var proxyControl = TerminalProxyManager.getProxy();
        if (proxyControl.isPresent()) {
            var proxyMultiplexerCommand = multiplexer
                    .get()
                    .launchNewSession(proxyControl.get(), launchConfiguration)
                    .toString();
            var proxyLaunchCommand = proxyControl
                    .get()
                    .prepareIntermediateTerminalOpen(
                            TerminalInitFunction.fixed(proxyMultiplexerCommand),
                            TerminalInitScriptConfig.ofName(AppNames.ofCurrent().getName()),
                            WorkingDirectoryFunction.none());
            // Restart for the next time
            proxyControl.get().start();
            var fullLocalCommand = getTerminalRegisterCommand(launchRequest, LocalShell.getDialect()) + "\n" + proxyLaunchCommand;
            var pane = new TerminalPaneConfiguration(multiplexerLaunchRequest, AppNames.ofCurrent().getName(), 0, fullLocalCommand, LocalShell.getDialect());
            return Optional.of(new TerminalLaunchConfiguration(
                    null,
                    AppNames.ofCurrent().getName(),
                    AppNames.ofCurrent().getName(),
                    false,
                    List.of(pane)));
        } else {
            var multiplexerCommand = multiplexer
                    .get()
                    .launchNewSession(LocalShell.getShell(), launchConfiguration)
                    .toString();
            var launchCommand = LocalShell.getShell()
                    .prepareIntermediateTerminalOpen(
                            TerminalInitFunction.fixed(multiplexerCommand),
                            TerminalInitScriptConfig.ofName(AppNames.ofCurrent().getName()),
                            WorkingDirectoryFunction.none());
            var fullLocalCommand = getTerminalRegisterCommand(launchRequest, LocalShell.getDialect()) + "\n" + launchCommand;
            var pane = new TerminalPaneConfiguration(multiplexerLaunchRequest, AppNames.ofCurrent().getName(), 0, fullLocalCommand, LocalShell.getDialect());
            return Optional.of(new TerminalLaunchConfiguration(
                    null,
                    AppNames.ofCurrent().getName(),
                    AppNames.ofCurrent().getName(),
                    false,
                    List.of(pane)));
        }
    }

    private static Optional<TerminalLaunchConfiguration> launchProxy(
            TerminalLaunchConfiguration launchConfiguration, UUID launchRequest) throws Exception {
        var proxyControl = TerminalProxyManager.getProxy();
        if (proxyControl.isEmpty()) {
            return Optional.empty();
        }

        var panes = new ArrayList<TerminalPaneConfiguration>();
        for (TerminalPaneConfiguration pane : launchConfiguration.getPanes()) {
            var openCommand = pane.getDialectLaunchCommand().buildSimple();
            var launchCommand = proxyControl
                    .get()
                    .prepareIntermediateTerminalOpen(
                            TerminalInitFunction.fixed(openCommand),
                            TerminalInitScriptConfig.ofName(AppNames.ofCurrent().getName()),
                            WorkingDirectoryFunction.none());
            var fullLocalCommand = getTerminalRegisterCommand(launchRequest, LocalShell.getDialect()) + "\n" + launchCommand;
            // Restart for the next time
            proxyControl.get().start();
            panes.add(pane.withScript(LocalShell.getDialect(), fullLocalCommand));
        }

        return Optional.ofNullable(launchConfiguration.withPanes(panes));
    }

    public static String getTerminalRegisterCommand(UUID request, ShellDialect dialect) throws Exception {
        var exec = AppInstallation.ofCurrent().getCliExecutablePath();
        var registerLine = CommandBuilder.of()
                .addFile(exec)
                .add("terminal-register", "--request", request.toString())
                .buildSimple();
        var powershell = ShellDialects.isPowershell(dialect);
        var bellLine = "printf \"\\a\"";
        var printBell = OsType.ofLocal() != OsType.WINDOWS
                && AppPrefs.get().enableTerminalStartupBell().get();
        var lines = ShellScript.lines((powershell ? "& " + registerLine : registerLine), printBell ? bellLine : null);
        return lines.toString();
    }

}
