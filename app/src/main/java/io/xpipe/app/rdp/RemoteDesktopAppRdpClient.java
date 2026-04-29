package io.xpipe.app.rdp;

import io.xpipe.app.core.AppDisplayScale;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.RdpConfig;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@JsonTypeName("microsoftRemoteDesktopApp")
@Value
@Jacksonized
@Builder
public class RemoteDesktopAppRdpClient implements ExternalApplicationType.MacApplication, ExternalRdpClient {

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var adjusted = AppDisplayScale.getEffectiveDisplayScale() >= 2.0 ?
                configuration.getConfig().overlay(Map.of("ForceHiDpiOptimizations", new RdpConfig.TypedValue("i", "1"))) :
                configuration.getConfig();
        var file = writeRdpConfigFile(configuration.getTitle(), adjusted);

        LocalShell.getShell()
                .executeSimpleCommand(CommandBuilder.of()
                        .add("open", "-a")
                        .addQuoted("Microsoft Remote Desktop.app")
                        .addFile(file.toString()));
    }

    @Override
    public boolean supportsPasswordPassing(RdpLaunchConfig config) {
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
}
