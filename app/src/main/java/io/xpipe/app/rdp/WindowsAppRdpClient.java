package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("windowsApp")
@Value
@Jacksonized
@Builder
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
    public String getWebsite() {
        return "https://learn.microsoft.com/en-us/windows-app/get-started-connect-devices-desktops-apps?tabs=windows-avd%2Cwindows-w365%2Cwindows"
                + "-devbox%2Cmacos-rds%2Cmacos-pc&pivots=remote-pc";
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
