package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.SshLocalBridge;
import io.xpipe.core.process.CommandBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class SecureCrtTerminalType extends ExternalTerminalType.WindowsType {

    public SecureCrtTerminalType() {
        super("app.secureCrt", "SecureCRT");
    }

    @Override
    public TerminalOpenFormat getOpenFormat() {
        return TerminalOpenFormat.TABBED;
    }

    @Override
    protected Optional<Path> determineInstallation() {
        try (var sc = LocalShell.getShell().start()) {
            var env = sc.executeSimpleStringCommand(
                    sc.getShellDialect().getPrintEnvironmentVariableCommand("ProgramFiles"));
            var file = Path.of(env, "VanDyke Software\\SecureCRT\\SecureCRT.exe");
            if (!Files.exists(file)) {
                return Optional.empty();
            }

            return Optional.of(file);
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
        return false;
    }

    @Override
    public String getWebsite() {
        return "https://www.vandyke.com/products/securecrt/";
    }

    @Override
    protected void execute(Path file, TerminalLaunchConfiguration configuration) throws Exception {
        try (var sc = LocalShell.getShell()) {
            SshLocalBridge.init();
            var b = SshLocalBridge.get();
            var command = CommandBuilder.of()
                    .addFile(file.toString())
                    .add("/T")
                    .add("/SSH2", "/ACCEPTHOSTKEYS", "/I")
                    .addFile(b.getIdentityKey().toString())
                    .add("/P", "" + b.getPort())
                    .add("/L")
                    .addQuoted(b.getUser())
                    .add("localhost");
            sc.executeSimpleCommand(command);
        }
    }
}
