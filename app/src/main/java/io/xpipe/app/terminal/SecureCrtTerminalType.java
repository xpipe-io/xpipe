package io.xpipe.app.terminal;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.SshLocalBridge;
import io.xpipe.core.process.CommandBuilder;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

public class SecureCrtTerminalType implements ExternalApplicationType.WindowsType, ExternalTerminalType {

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
        return "SecureCRT";
    }

    @Override
    public Optional<Path> determineInstallation() {
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
    public void launch(TerminalLaunchConfiguration configuration) throws Exception {
        try (var sc = LocalShell.getShell()) {
            SshLocalBridge.init();
            var b = SshLocalBridge.get();
            var command = CommandBuilder.of()
                    .add("/T")
                    .add("/SSH2", "/ACCEPTHOSTKEYS", "/I")
                    .addFile(b.getIdentityKey().toString())
                    .add("/P", "" + b.getPort())
                    .add("/L")
                    .addQuoted(b.getUser())
                    .add("localhost");
            launch(command);
        }
    }

    @Override
    public String getWebsite() {
        return "https://www.vandyke.com/products/securecrt/";
    }

    @Override
    public String getId() {
        return "app.secureCrt";
    }
}
