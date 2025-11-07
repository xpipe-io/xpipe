package io.xpipe.app.rdp;

import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.process.CommandSupport;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.util.FlatpakCache;
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
        CommandBuilder exec;
        var v3 = CommandSupport.isInLocalPath("xfreerdp3");
        if (!v3) {
            var v2 = CommandSupport.isInLocalPath("xfreerdp");
            if (!v2 && OsType.ofLocal() == OsType.LINUX) {
                var flatpak = FlatpakCache.getApp("com.freerdp.FreeRDP");
                if (flatpak.isPresent()) {
                    exec = FlatpakCache.runCommand("com.freerdp.FreeRDP");
                    v3 = true;
                } else {
                    CommandSupport.isInPathOrThrow(LocalShell.getShell(), "xfreerdp");
                    exec = CommandBuilder.of().add("xfreerdp");
                }
            } else {
                CommandSupport.isInPathOrThrow(LocalShell.getShell(), "xfreerdp");
                exec = CommandBuilder.of().add("xfreerdp");
                // macOS uses xfreerdp3 by default
                v3 = true;
            }
        } else {
            exec = CommandBuilder.of().add("xfreerdp3");
        }

        var file = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
        var b = CommandBuilder.of()
                .add(exec)
                .addFile(file.toString())
                .add(v3 ? "/cert:ignore" : "/cert-ignore")
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
