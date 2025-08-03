package io.xpipe.app.vnc;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.RemminaHelper;
import io.xpipe.app.util.ThreadHelper;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;
import org.apache.commons.io.FileUtils;

import java.util.Optional;

@Builder
@Jacksonized
@JsonTypeName("remmina")
public class RemminaVncClient implements ExternalApplicationType.PathApplication, ExternalVncClient {

    @Override
    public String getWebsite() {
        return "https://remmina.org/";
    }

    @Override
    public boolean supportsPasswords() {
        return true;
    }

    @Override
    public String getExecutable() {
        return "remmina";
    }

    @Override
    public boolean detach() {
        return true;
    }

    @Override
    public void launch(VncLaunchConfig configuration) throws Exception {
        var pw = configuration.retrievePassword();
        var encrypted = pw.isPresent() ? RemminaHelper.encryptPassword(pw.get()) : Optional.<String>empty();
        if (encrypted.isPresent()) {
            var file = RemminaHelper.writeRemminaVncConfigFile(configuration, encrypted.get());
            launch(CommandBuilder.of().add("-c").addFile(file.toString()));
            ThreadHelper.runFailableAsync(() -> {
                ThreadHelper.sleep(5000);
                FileUtils.deleteQuietly(file.toFile());
            });
        }
    }
}
