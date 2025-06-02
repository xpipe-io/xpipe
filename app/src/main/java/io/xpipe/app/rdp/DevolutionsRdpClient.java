package io.xpipe.app.rdp;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.app.util.WindowsRegistry;
import io.xpipe.core.process.CommandBuilder;
import org.apache.commons.io.FileUtils;

import java.nio.file.Path;
import java.util.Optional;

public class DevolutionsRdpClient implements ExternalApplicationType.WindowsType, ExternalRdpClient {

    @Override
    public String getExecutable() {
        return "RemoteDesktopManager";
    }

    @Override
    public Optional<Path> determineInstallation() {
        try {
            var r = WindowsRegistry.local().readStringValueIfPresent(WindowsRegistry.HKEY_LOCAL_MACHINE, "SOFTWARE\\Classes\\rdm\\DefaultIcon");
            return r.map(Path::of);
        } catch (Exception e) {
            ErrorEvent.fromThrowable(e).omit().handle();
            return Optional.empty();
        }
    }

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var location = findExecutable();
        var config = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
        LocalShell.getShell().executeSimpleCommand(CommandBuilder.of().addFile(location).addFile(config).discardAllOutput());
        ThreadHelper.runFailableAsync(() -> {
            // Startup is slow
            ThreadHelper.sleep(10000);
            FileUtils.deleteQuietly(config.toFile());
        });
    }

    @Override
    public boolean supportsPasswordPassing() {
        return false;
    }

    @Override
    public String getId() {
        return "app.devolutions";
    }
}
