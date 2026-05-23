package io.xpipe.app.vnc;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
@JsonTypeName("krdc")
public class KrdcVncClient implements ExternalApplicationType.LinuxApplication, ExternalVncClient {

    @Override
    public String getExecutable() {
        return "krdc";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public void launch(VncLaunchConfig configuration) throws Exception {
        launch(CommandBuilder.of().addQuoted("vnc://" + configuration.getHost() + ":" + configuration.getPort()));
    }

    @Override
    public boolean supportsPasswords() {
        return false;
    }

    @Override
    public String getWebsite() {
        return "https://apps.kde.org/krdc/";
    }

    @Override
    public String getFlatpakId() {
        return "org.kde.krdc";
    }
}
