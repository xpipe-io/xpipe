package io.xpipe.app.terminal;

import io.xpipe.app.core.AppLocations;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.SshLocalBridge;

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
        var file = AppLocations.getWindows().getProgramFiles().resolve("VanDyke Software\\SecureCRT\\SecureCRT.exe");
        if (!Files.exists(file)) {
            return Optional.empty();
        }

        return Optional.of(file);
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

    @Override
    public String getWebsite() {
        return "https://www.vandyke.com/products/securecrt/";
    }

    @Override
    public String getId() {
        return "app.secureCrt";
    }
}
