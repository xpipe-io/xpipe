package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.CommandSupport;
import io.xpipe.app.util.LocalShell;
import io.xpipe.core.OsType;

public class FreeRdpClient implements ExternalApplicationType.PathApplication, ExternalRdpClient {

    @Override
    public String getWebsite() {
        return "https://www.freerdp.com/";
    }

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        CommandSupport.isInPathOrThrow(
                LocalShell.getShell(),
                getExecutable(),
                "XFreeRDP",
                DataStorage.get().local());

        var file = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
        var b = CommandBuilder.of()
                .add(getExecutable())
                .addFile(file.toString())
                .add(OsType.getLocal() == OsType.LINUX ? "/cert-ignore" : "/cert:ignore")
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
    public String getExecutable() {
        return "xfreerdp";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public String getId() {
        return "app.xfreeRdp";
    }
}
