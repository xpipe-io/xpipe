package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.core.OsType;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("freeRdp")
@Value
@Jacksonized
@Builder
public class FreeRdpClient implements ExternalRdpClient {

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var v3 = LocalShell.getShell().view().findProgram("xfreerdp3");
        if (v3.isEmpty()) {
            CommandSupport.isInPathOrThrow(LocalShell.getShell(), getExecutable(), "xfreerdp", DataStorage.get().local());
        }

        var file = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
        // macOS uses xfreerdp3 by default
        var isV3Executable = v3.isPresent() || OsType.ofLocal() == OsType.MACOS;
        var b = CommandBuilder.of()
                .add(v3.isPresent() ? "xfreerdp3" : "xfreerdp")
                .addFile(file.toString())
                .add(isV3Executable ? "/cert:ignore" : "/cert-ignore")
                .add("/dynamic-resolution")
                .add("/network:auto")
                .add("/compression")
                .add("+clipboard")
                .add("-themes")
                .add("/size:100%");

        if (configuration.getPassword() != null) {
            var escapedPw = configuration.getPassword().getSecretValue().replaceAll("'", "\\\\'");
            b.add("/p:'" + escapedPw + "'");
        }

        try (var sc = LocalShell.getShell().start()) {
            var cmd = sc.getShellDialect().launchAsnyc(b);
            sc.command(cmd).sensitive().execute();
        }
    }

    @Override
    public boolean supportsPasswordPassing() {
        return true;
    }

    @Override
    public String getWebsite() {
        return "https://www.freerdp.com/";
    }

    @Override
    public String getId() {
        return "app.xfreeRdp";
    }
}
