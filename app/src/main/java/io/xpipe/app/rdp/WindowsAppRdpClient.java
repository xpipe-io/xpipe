package io.xpipe.app.rdp;

import io.xpipe.app.core.AppDisplayScale;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.LombokHelper;
import io.xpipe.app.util.RdpConfig;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.util.ThreadHelper;
import javafx.application.Platform;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Map;

@JsonTypeName("windowsApp")
@Jacksonized
@Builder
@Getter
public class WindowsAppRdpClient implements ExternalApplicationType.MacApplication, ExternalRdpClient {

    private final boolean hidpi;

    @SuppressWarnings("unused")
    static OptionsBuilder createOptions(Property<WindowsAppRdpClient> property) {
        var hidpi = new SimpleObjectProperty<>(property.getValue().isHidpi());

        return new OptionsBuilder()
                .nameAndDescription("rdpHidpi")
                .addToggle(hidpi)
                .bind(
                        () -> {
                            return WindowsAppRdpClient.builder().hidpi(hidpi.get()).build();
                        },
                        property);
    }

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var optimizeHidpi = hidpi && AppDisplayScale.getEffectiveDisplayScale() >= 2.0 && configuration.getConfig().get(
                "ForceHiDpiOptimizations").isEmpty();
        var adjusted = optimizeHidpi
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
}
