package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.LocalShell;

public class RemoteDesktopAppRdpClient implements ExternalApplicationType.MacApplication, ExternalRdpClient {

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var file = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
        LocalShell.getShell()
                .executeSimpleCommand(CommandBuilder.of()
                        .add("open", "-a")
                        .addQuoted("Microsoft Remote Desktop.app")
                        .addFile(file.toString()));
    }

    @Override
    public boolean supportsPasswordPassing() {
        return false;
    }

    @Override
    public String getApplicationName() {
        return "Microsoft Remote Desktop";
    }

    @Override
    public String getId() {
        return "app.microsoftRemoteDesktopApp";
    }
}
