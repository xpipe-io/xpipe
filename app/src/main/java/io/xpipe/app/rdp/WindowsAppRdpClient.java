package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.process.CommandBuilder;

public class WindowsAppRdpClient implements ExternalApplicationType.MacApplication, ExternalRdpClient {

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var file = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
        LocalShell.getShell()
                .executeSimpleCommand(CommandBuilder.of()
                        .add("open", "-a")
                        .addQuoted("Windows App.app")
                        .addFile(file.toString()));
    }

    @Override
    public boolean supportsPasswordPassing() {
        return false;
    }

    @Override
    public String getApplicationName() {
        return "Windows App";
    }

    @Override
    public String getId() {
        return "app.windowsApp";
    }
}
