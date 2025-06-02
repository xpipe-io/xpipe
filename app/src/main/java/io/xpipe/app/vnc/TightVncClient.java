package io.xpipe.app.vnc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.process.CommandBuilder;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Builder
@Jacksonized
@JsonTypeName("tightVnc")
public class TightVncClient implements ExternalApplicationType.WindowsType, ExternalVncClient {

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getId() {
        return null;
    }

    @Override
    public String getExecutable() {
        return "tvnviewer.exe";
    }

    @Override
    public Optional<Path> determineInstallation() {
        return Optional.of(Path.of(System.getenv("PROGRAMFILES"))
                        .resolve("TightVNC")
                        .resolve("tvnviewer.exe"))
                .filter(path -> Files.exists(path));
    }

    @Override
    public void launch(LaunchConfiguration configuration) throws Exception {
        var command = CommandBuilder.of().addFile(findExecutable()).addQuoted(configuration.getHost() + "::" + configuration.getPort());
        LocalShell.getShell().command(command).execute();
    }
}
