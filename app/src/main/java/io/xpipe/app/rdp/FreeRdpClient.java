package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.core.OsType;

public class FreeRdpClient implements ExternalApplicationType.PathApplication, ExternalRdpClient {

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        var file = writeRdpConfigFile(configuration.getTitle(), configuration.getConfig());
        var b = CommandBuilder.of().addFile(file.toString()).add(OsType.getLocal() == OsType.LINUX ? "/cert-ignore" : "/cert:ignore");
        if (configuration.getPassword() != null) {
            var escapedPw = configuration.getPassword().getSecretValue().replaceAll("'", "\\\\'");
            b.add("/p:'" + escapedPw + "'");
        }
        launch(b);
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
