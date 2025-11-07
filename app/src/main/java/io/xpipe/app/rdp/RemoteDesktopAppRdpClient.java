package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("microsoftRemoteDesktopApp")
@Value
@Jacksonized
@Builder
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
    public String getWebsite() {
        return "https://learn.microsoft.com/en-us/previous-versions/remote-desktop-client/remote-desktop-macos";
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
