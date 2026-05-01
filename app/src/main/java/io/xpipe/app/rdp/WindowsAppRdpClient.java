package io.xpipe.app.rdp;

import io.xpipe.app.core.AppDisplayScale;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.RdpConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@JsonTypeName("windowsApp")
@Value
@Jacksonized
@Builder
public class WindowsAppRdpClient implements ExternalApplicationType.MacApplication, ExternalRdpClient {

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var adjusted = AppDisplayScale.getEffectiveDisplayScale() >= 2.0
                ? configuration
                        .getConfig()
                        .overlay(Map.of("ForceHiDpiOptimizations", new RdpConfig.TypedValue("i", "1")))
                : configuration.getConfig();
        var file = writeRdpConfigFile(configuration.getTitle(), adjusted);
        LocalShell.getShell()
                .executeSimpleCommand(CommandBuilder.of()
                        .add("open", "-a")
                        .addQuoted("Windows App.app")
                        .addFile(file.toString()));
    }

    @Override
    public boolean supportsPasswordPassing(RdpLaunchConfig config) {
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
}
