package io.xpipe.app.terminal;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellDialects;
import io.xpipe.core.store.FileNames;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.XPipeInstallation;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicInteger;

public interface WindowsTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType WINDOWS_TERMINAL = new Standard();
    ExternalTerminalType WINDOWS_TERMINAL_PREVIEW = new Preview();
    ExternalTerminalType WINDOWS_TERMINAL_CANARY = new Canary();

    AtomicInteger windowCounter = new AtomicInteger(2);

    private static CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
        var cmd = CommandBuilder.of()
                .addIf(configuration.isPreferTabs(), "-w", "1", "nt")
                .addIf(!configuration.isPreferTabs(), "-w", "" + windowCounter.getAndIncrement());

        if (configuration.getColor() != null) {
            cmd.add("--tabColor").addQuoted(configuration.getColor().toHexString());
        }

        // A weird behavior in Windows Terminal causes the trailing
        // backslash of a filepath to escape the closing quote in the title argument
        // So just remove that slash
        var fixedName = FileNames.removeTrailingSlash(configuration.getColoredTitle());
        cmd.add("--title").addQuoted(fixedName);
        cmd.add("--profile").addQuoted("{021eff0f-b38a-45f9-895d-41467e9d510f}");

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

    default void checkProfile() throws IOException {
        var profileSet = AppCache.getBoolean("wtProfileSet", false);
        if (profileSet) {
            return;
        }

        var uuid = "{021eff0f-b38a-45f9-895d-41467e9d510f}";
        var config = JacksonMapper.getDefault().readTree(getConfigFile().toFile());
        var profiles = config.withObjectProperty("profiles").withArrayProperty("list");
        for (int i = 0; i < profiles.size(); i++) {
            var profile = profiles.get(i);
            var profileId = profile.get("guid");
            if (profileId != null && profileId.asText().equals(uuid)) {
                profiles.remove(i);
                break;
            }
        }

        var newProfile = JsonNodeFactory.instance.objectNode();
        newProfile.put("guid", uuid);
        newProfile.put("hidden", true);
        newProfile.put("name", "XPipe");
        newProfile.put("closeOnExit", "always");
        newProfile.put("suppressApplicationTitle", true);
        if (!AppProperties.get().isDevelopmentEnvironment()) {
            var dir = XPipeInstallation.getLocalDefaultInstallationIcon();
            newProfile.put("icon", dir.toString());
        }
        profiles.add(newProfile);
        JacksonMapper.getDefault().writeValue(getConfigFile().toFile(), config);
        AppCache.update("wtProfileSet", true);
    }

    @Override
    default TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.NEW_WINDOW_OR_TABBED;
    }

    @Override
    default int getProcessHierarchyOffset() {
        var powershell = AppPrefs.get().enableTerminalLogging().get()
                && !ShellDialects.isPowershell(ProcessControlProvider.get().getEffectiveLocalDialect());
        return powershell ? 1 : 0;
    }

    @Override
    default boolean isRecommended() {
        return true;
    }

    @Override
    default boolean supportsColoredTitle() {
        return false;
    }

    Path getConfigFile();

    class Standard extends SimplePathType implements WindowsTerminalType {

        public Standard() {
            super("app.windowsTerminal", "wt.exe", false);
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            checkProfile();
            super.launch(configuration);
        }

        @Override
        public String getWebsite() {
            return "https://aka.ms/terminal";
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) throws Exception {
            return WindowsTerminalType.toCommand(configuration);
        }

        @Override
        public Path getConfigFile() {
            var local = System.getenv("LOCALAPPDATA");
            return Path.of(local)
                    .resolve("Packages\\Microsoft.WindowsTerminal_8wekyb3d8bbwe\\LocalState\\settings.json");
        }
    }

    class Preview implements WindowsTerminalType {

        @Override
        public String getWebsite() {
            return "https://aka.ms/terminal-preview";
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            if (!isAvailable()) {
                throw ErrorEvent.expected(
                        new IllegalArgumentException("Windows Terminal Preview is not installed at " + getPath()));
            }

            checkProfile();
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
            // The executable is a weird link
            return Files.exists(getPath().getParent());
        }

        @Override
        public String getId() {
            return "app.windowsTerminalPreview";
        }

        @Override
        public Path getConfigFile() {
            var local = System.getenv("LOCALAPPDATA");
            return Path.of(local)
                    .resolve("Packages\\Microsoft.WindowsTerminalPreview_8wekyb3d8bbwe\\LocalState\\settings.json");
        }
    }

    class Canary implements WindowsTerminalType {

        @Override
        public String getWebsite() {
            return "https://devblogs.microsoft.com/commandline/introducing-windows-terminal-canary/";
        }

        @Override
        public void launch(TerminalLaunchConfiguration configuration) throws Exception {
            if (!isAvailable()) {
                throw ErrorEvent.expected(
                        new IllegalArgumentException("Windows Terminal Canary is not installed at " + getPath()));
            }

            checkProfile();
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
            // The executable is a weird link
            return Files.exists(getPath().getParent());
        }

        @Override
        public String getId() {
            return "app.windowsTerminalCanary";
        }

        @Override
        public Path getConfigFile() {
            var local = System.getenv("LOCALAPPDATA");
            return Path.of(local)
                    .resolve("Packages\\Microsoft.WindowsTerminalCanary_8wekyb3d8bbwe\\LocalState\\settings.json");
        }
    }
}
