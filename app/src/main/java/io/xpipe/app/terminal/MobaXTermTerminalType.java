package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ScriptHelper;
import io.xpipe.app.util.SshLocalBridge;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.CommandBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class MobaXTermTerminalType extends ExternalTerminalType.WindowsType {

    public MobaXTermTerminalType() {
        super("app.mobaXterm", "MobaXterm");
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    protected Optional<Path> determineInstallation() {
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
    public boolean supportsColoredTitle() {
        return true;
    }

    @Override
    public String getWebsite() {
        return "https://mobaxterm.mobatek.net/";
    }

    @Override
    protected void execute(Path file, TerminalLaunchConfiguration configuration) throws Exception {
        try (var sc = LocalShell.getShell()) {
            SshLocalBridge.init();
            var b = SshLocalBridge.get();
            var command = CommandBuilder.of()
                    .addFile("ssh")
                    .addQuoted(b.getUser() + "@localhost")
                    .add("-i")
                    .add("\"$(cygpath \"" + b.getIdentityKey().toString() + "\")\"")
                    .add("-p")
                    .add("" + b.getPort());
            // Don't use local shell to build as it uses cygwin
            var rawCommand = command.buildSimple();
            var script = ScriptHelper.getExecScriptFile(sc, "sh");
            Files.writeString(Path.of(script.toString()), rawCommand);
            var fixedFile = script.toString().replaceAll("\\\\", "/").replaceAll("\\s", "\\$0");
            sc.command(CommandBuilder.of()
                            .addFile(file.toString())
                            .add("-newtab")
                            .add(fixedFile))
                    .execute();
        }
    }
}
