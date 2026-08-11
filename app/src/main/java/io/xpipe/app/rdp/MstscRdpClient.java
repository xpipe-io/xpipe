package io.xpipe.app.rdp;

import io.xpipe.app.core.AppSystemInfo;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.util.*;

import javafx.beans.property.Property;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Path;
import java.util.Optional;

@JsonTypeName("mstsc")
@Jacksonized
@SuperBuilder(toBuilder = true)
public class MstscRdpClient extends MicrosoftRdpClient {

    @SuppressWarnings("unused")
    public static MstscRdpClient createDefault() {
        return MstscRdpClient.builder().dock(true).smartSizing(true).build();
    }

    @SuppressWarnings("unused")
    static OptionsBuilder createOptions(Property<MstscRdpClient> property) {
        return MicrosoftRdpClient.createSharedOptions(property);
    }

    @Override
    public String getWebsite() {
        return "https://learn.microsoft.com/en-us/windows-server/administration/windows-commands/mstsc";
    }

    @Override
    public String getExecutable() {
        return "mstsc.exe";
    }

    @Override
    public Optional<Path> determineInstallation() {
        return Optional.of(AppSystemInfo.ofWindows().getSystemRoot().resolve("System32", "mstsc.exe"));
    }
}
