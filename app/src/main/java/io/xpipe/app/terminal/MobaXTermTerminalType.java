package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.*;
import io.xpipe.core.process.CommandBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class MobaXTermTerminalType implements ExternalApplicationType.WindowsType, ExternalTerminalType {

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    public boolean detach() {
        return false;
    }

    @Override
    public String getExecutable() {
        return "MobaXterm";
    }

    @Override
    public Optional<Path> determineInstallation() {
        try {
            var r = WindowsRegistry.local()
                    .readStringValueIfPresent(
                            WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Classes\\mobaxterm\\DefaultIcon");
            return r.map(Path::of);
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).omit().handle();
            return Optional.empty();
        }
    }

    @Override
    public boolean isRecommended() {
        return false;
    }

    @Override
    public boolean useColoredTitle() {
        return true;
    }

    @Override
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        try (var sc = LocalShell.getShell()) {
            SshLocalBridge.init();
            var b = SshLocalBridge.get();
            var abs = b.getIdentityKey().toAbsolutePath();
            var drivePath =
                    "/drives/" + abs.getRoot().toString().substring(0, 1).toLowerCase() + "/"
                            + abs.getRoot().relativize(abs).toString().replaceAll("\\\\", "/");
            var winPath = b.getIdentityKey().toString().replaceAll("\\\\", "\\\\\\\\");
            var command = CommandBuilder.of()
                    .add("ssh")
                    .addQuoted(b.getUser() + "@localhost")
                    .add("-i")
                    .add("\"$(cygpath -u \"" + winPath + "\" || echo \"" + drivePath + "\")\"")
                    .add("-p")
                    .add("" + b.getPort());
            // Don't use local shell to build as it uses cygwin
            var rawCommand = command.buildSimple();
            var script = ShellTemp.getLocalTempDataDirectory("mobaxpipe.sh");
            Files.writeString(Path.of(script.toString()), "#!/usr/bin/env bash\n" + rawCommand);
            var fixedFile = script.toString().replaceAll("\\\\", "/").replaceAll("\\s", "\\$0");
            launch(CommandBuilder.of().add("-newtab").add(fixedFile));
        }
    }

    @Override
    public String getWebsite() {
        return "https://mobaxterm.mobatek.net/";
    }

    @Override
    public String getId() {
        return "app.mobaXterm";
    }
}
