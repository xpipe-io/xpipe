package io.xpipe.app.rdp;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.platform.OptionsBuilder;

import javafx.beans.property.Property;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@JsonTypeName("msrdc")
@Jacksonized
@SuperBuilder(toBuilder = true)
public class MsrdcRdpClient extends MicrosoftRdpClient {

    @SuppressWarnings("unused")
    public static MsrdcRdpClient createDefault() {
        return MsrdcRdpClient.builder().dock(true).smartSizing(true).build();
    }

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(Property<MsrdcRdpClient> property) {
        return MicrosoftRdpClient.createSharedOptions(property);
    }

    @Override
    public String getWebsite() {
        return "https://learn.microsoft.com/en-us/previous-versions/remote-desktop-client/connect-windows-cloud-services?tabs=windows-msrdc-msi";
    }

    @Override
    public String getExecutable() {
        // We don't want to use an exe in the PATH as that one might come from WSL
        return null;
    }

    @Override
    public Optional<Path> determineInstallation() {
        var exe = AppSystemInfo.ofWindows().getLocalAppData().resolve("Apps", "Remote Desktop", "msrdc.exe");
        if (Files.exists(exe)) {
            return Optional.of(exe);
        }

        return Optional.empty();
    }
}
