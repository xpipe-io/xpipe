package io.xpipe.app.terminal;

import io.xpipe.app.core.AppCache;
import io.xpipe.app.core.AppInstallation;
import io.xpipe.app.core.AppProperties;
import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.FilePath;
import io.xpipe.core.JacksonMapper;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicInteger;

public interface WindowsTerminalType extends ExternalTerminalType, TrackableTerminalType {

    ExternalTerminalType WINDOWS_TERMINAL = new Standard();
    ExternalTerminalType WINDOWS_TERMINAL_PREVIEW = new Preview();
    ExternalTerminalType WINDOWS_TERMINAL_CANARY = new Canary();

    AtomicInteger windowCounter = new AtomicInteger(10);

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
        var fixedName = FilePath.of(configuration.getColoredTitle())
                .removeTrailingSlash()
                .toString();
        // To fix https://github.com/microsoft/terminal/issues/13264
        fixedName = fixedName.replaceAll(";", "_");

        cmd.add("--title").addQuoted(fixedName);
        cmd.add("--profile").addQuoted("{021eff0f-b38a-45f9-895d-41467e9d510f}");

        var spaces = configuration.getScriptFile().toString().contains(" ");
        cmd.add(configuration
                .getScriptDialect()
                .getOpenScriptCommand(spaces ? configuration.getScriptFile().getFileName() : configuration.getScriptFile().toString()));
        return cmd;
    }

    default void checkProfile() throws IOException {
        // Update old configs
        var before =
                LocalDate.of(2025, 4, 2).atStartOfDay(ZoneId.systemDefault()).toInstant();
        var outdated = AppCache.getModifiedTime("wtProfileSet")
                .map(instant -> instant.isBefore(before))
                .orElse(false);

        var profileSet = AppCache.getBoolean("wtProfileSet", false);
        if (profileSet && !outdated) {
            return;
        }

        if (outdated) {
            AppCache.clear("wtProfileSet");
        }

        if (!Files.exists(getConfigFile())) {
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
        newProfile.putNull("startingDirectory");
        newProfile.put("elevate", false);
        if (!AppProperties.get().isDevelopmentEnvironment()) {
            var logoFile = AppInstallation.ofCurrent().getLogoPath();
            newProfile.put("icon", logoFile.toString());
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
    default boolean isRecommended() {
        return true;
    }

    @Override
    default boolean useColoredTitle() {
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
            try (var sc = LocalShell.getShell().start()) {
                var inPath = sc.view().findProgram("wt");
                var exec = inPath.orElse(FilePath.of(getPath()));
                var spaces = configuration.getScriptFile().toString().contains(" ");

                if (spaces) {
                    var wd = sc.view().pwd();
                    sc.command(CommandBuilder.of().addFile(exec).add(toCommand(configuration))).withWorkingDirectory(
                            configuration.getScriptFile().getParent()).execute();
                    sc.view().cd(wd);
                } else {
                    sc.command(CommandBuilder.of().addFile(exec).add(toCommand(configuration))).execute();
                }
            }
        }

        @Override
        protected CommandBuilder toCommand(TerminalLaunchConfiguration configuration) {
            return WindowsTerminalType.toCommand(configuration);
        }

        @Override
        public String getWebsite() {
            return "https://aka.ms/terminal";
        }

        private Path getPath() {
            return AppSystemInfo.ofWindows()
                    .getLocalAppData()
                    .resolve("Microsoft\\WindowsApps\\Microsoft.WindowsTerminal_8wekyb3d8bbwe\\wt.exe");
        }

        @Override
        public Path getConfigFile() {
            return AppSystemInfo.ofWindows()
                    .getLocalAppData()
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
                throw ErrorEventFactory.expected(
                        new IllegalArgumentException("Windows Terminal Preview is not installed at " + getPath()));
            }

            checkProfile();
            try (var sc = LocalShell.getShell().start()) {
                var exec = getPath();
                var spaces = configuration.getScriptFile().toString().contains(" ");

                if (spaces) {
                    var wd = sc.view().pwd();
                    sc.command(CommandBuilder.of().addFile(exec).add(toCommand(configuration)))
                            .withWorkingDirectory(configuration.getScriptFile().getParent())
                            .execute();
                    sc.view().cd(wd);
                } else {
                    sc.command(CommandBuilder.of().addFile(exec).add(toCommand(configuration))).execute();
                }
            }
        }

        private Path getPath() {
            return AppSystemInfo.ofWindows()
                    .getLocalAppData()
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
            return AppSystemInfo.ofWindows()
                    .getLocalAppData()
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
                throw ErrorEventFactory.expected(
                        new IllegalArgumentException("Windows Terminal Canary is not installed at " + getPath()));
            }

            checkProfile();
            try (var sc = LocalShell.getShell().start()) {
                var exec = getPath();
                var spaces = configuration.getScriptFile().toString().contains(" ");

                if (spaces) {
                    var wd = sc.view().pwd();
                    sc.command(CommandBuilder.of().addFile(exec).add(toCommand(configuration))).withWorkingDirectory(
                            configuration.getScriptFile().getParent()).execute();
                    sc.view().cd(wd);
                } else {
                    sc.command(CommandBuilder.of().addFile(exec).add(toCommand(configuration))).execute();
                }
            }
        }

        private Path getPath() {
            return AppSystemInfo.ofWindows()
                    .getLocalAppData()
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
            return AppSystemInfo.ofWindows()
                    .getLocalAppData()
                    .resolve("Packages\\Microsoft.WindowsTerminalCanary_8wekyb3d8bbwe\\LocalState\\settings.json");
        }
    }
}
