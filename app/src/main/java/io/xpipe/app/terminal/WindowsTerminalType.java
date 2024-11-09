package io.xpipe.app.terminal;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FileNames;

import java.nio.file.Files;
import java.nio.file.Path;

public interface WindowsTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType WINDOWS_TERMINAL = new Standard();
    ExternalTerminalType WINDOWS_TERMINAL_PREVIEW = new Preview();
    ExternalTerminalType WINDOWS_TERMINAL_CANARY = new Canary();

    private static CommandBuilder toCommand(ExternalTerminalType.LaunchConfiguration configuration) throws Exception {
        var cmd = CommandBuilder.of().add("-w", "1", "nt");

        if (configuration.getColor() != null) {
            cmd.add("--tabColor").addQuoted(configuration.getColor().toHexString());
        }

        // A weird behavior in Windows Terminal causes the trailing
        // backslash of a filepath to escape the closing quote in the title argument
        // So just remove that slash
        var fixedName = FileNames.removeTrailingSlash(configuration.getColoredTitle());
        cmd.add("--title").addQuoted(fixedName);

        // wt can't elevate a command consisting out of multiple parts if wt is configured to elevate by default
        // So work around it by just passing a script file if possible
        if (ShellDialects.isPowershell(configuration.getScriptDialect())) {
            var usesPowershell =
                    ShellDialects.isPowershell(ProcessControlProvider.get().getEffectiveLocalDialect());
            if (usesPowershell) {
                // We can't work around it in this case, so let's just hope that there's no elevation configured
                cmd.add(configuration.getDialectLaunchCommand());
            } else {
                // There might be a mismatch if we are for example using logging
                // In this case we can actually work around the problem
                cmd.addFile(shellControl -> {
                    var script = ScriptHelper.createExecScript(
                            shellControl,
                            configuration.getDialectLaunchCommand().buildFull(shellControl));
                    return script.toString();
                });
            }
        } else {
            cmd.addFile(configuration.getScriptFile());
        }

        return cmd;
    }

    @Override
    default int getProcessHierarchyOffset() {
        var powershell = AppPrefs.get().enableTerminalLogging().get() && !ShellDialects.isPowershell(ProcessControlProvider.get().getEffectiveLocalDialect());
        return powershell ? 1 : 0;
    }

    @Override
    default boolean supportsTabs() {
        return true;
    }

    @Override
    default boolean isRecommended() {
        return true;
    }

    @Override
    default boolean supportsColoredTitle() {
        return false;
    }

    class Standard extends SimplePathType implements WindowsTerminalType {

        public Standard() {
            super("app.windowsTerminal", "wt.exe", false);
        }

        @Override
        public String getWebsite() {
            return "https://aka.ms/terminal";
        }

        @Override
        protected CommandBuilder toCommand(LaunchConfiguration configuration) throws Exception {
            return WindowsTerminalType.toCommand(configuration);
        }
    }

    class Preview implements WindowsTerminalType {

        @Override
        public String getWebsite() {
            return "https://aka.ms/terminal-preview";
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            if (!isAvailable()) {
                throw ErrorEvent.expected(new IllegalArgumentException("Windows Terminal Preview is not installed"));
            }

            LocalShell.getShell()
                    .executeSimpleCommand(
                            CommandBuilder.of().addFile(getPath().toString()).add(toCommand(configuration)));
        }

        private Path getPath() {
            var local = System.getenv("LOCALAPPDATA");
            return Path.of(local)
                    .resolve("Microsoft\\WindowsApps\\Microsoft.WindowsTerminalPreview_8wekyb3d8bbwe\\wt.exe");
        }

        @Override
        public boolean isAvailable() {
            return Files.exists(getPath());
        }

        @Override
        public String getId() {
            return "app.windowsTerminalPreview";
        }
    }

    class Canary implements WindowsTerminalType {

        @Override
        public String getWebsite() {
            return "https://devblogs.microsoft.com/commandline/introducing-windows-terminal-canary/";
        }

        @Override
        public void launch(LaunchConfiguration configuration) throws Exception {
            if (!isAvailable()) {
                throw ErrorEvent.expected(new IllegalArgumentException("Windows Terminal Canary is not installed"));
            }

            LocalShell.getShell()
                    .executeSimpleCommand(
                            CommandBuilder.of().addFile(getPath().toString()).add(toCommand(configuration)));
        }

        private Path getPath() {
            var local = System.getenv("LOCALAPPDATA");
            return Path.of(local)
                    .resolve("Microsoft\\WindowsApps\\Microsoft.WindowsTerminalCanary_8wekyb3d8bbwe\\wt.exe");
        }

        @Override
        public boolean isAvailable() {
            return Files.exists(getPath());
        }

        @Override
        public String getId() {
            return "app.windowsTerminalCanary";
        }
    }
}
