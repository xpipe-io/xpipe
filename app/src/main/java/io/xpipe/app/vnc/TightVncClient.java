package io.xpipe.app.vnc;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.LocalShell;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

@Builder
@Jacksonized
@JsonTypeName("tightVnc")
public class TightVncClient implements ExternalApplicationType.InstallLocationType, ExternalVncClient {

    @Override
    public String getWebsite() {
        return "https://www.tightvnc.com";
    }

    @Override
    public boolean supportsPasswords() {
        return true;
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
    public void launch(VncLaunchConfig configuration) throws Exception {
        var builder = CommandBuilder.of()
                .addFile(findExecutable())
                .addQuotedKeyValue("-host", configuration.getHost())
                .addQuotedKeyValue("-port", "" + configuration.getPort());
        var pw = configuration.retrievePassword();
        pw.ifPresent(secretValue -> builder.addQuotedKeyValue("-password", secretValue.getSecretValue()));
        var command = LocalShell.getShell().command(builder);
        if (pw.isPresent()) {
            command.sensitive();
        }
        command.execute();
    }
}
