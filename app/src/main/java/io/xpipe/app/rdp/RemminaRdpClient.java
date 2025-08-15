package io.xpipe.app.rdp;

import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.*;

import org.apache.commons.io.FileUtils;

import java.util.*;

public class RemminaRdpClient implements ExternalApplicationType.PathApplication, ExternalRdpClient {

    private List<String> toStrip() {
        return List.of("auto connect", "password 51", "prompt for credentials", "smart sizing");
    }

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        RdpConfig c = configuration.getConfig();
        var l = new HashSet<>(c.getContent().keySet());
        toStrip().forEach(l::remove);
        if (l.size() == 2 && l.contains("username") && l.contains("full address")) {
            var encrypted = RemminaHelper.encryptPassword(configuration.getPassword());
            if (encrypted.isPresent()) {
                var file = RemminaHelper.writeRemminaRdpConfigFile(configuration, encrypted.get());
                launch(CommandBuilder.of().add("-c").addFile(file.toString()));
                ThreadHelper.runFailableAsync(() -> {
                    ThreadHelper.sleep(5000);
                    FileUtils.deleteQuietly(file.toFile());
                });
                return;
            }
        }

        var file = writeRdpConfigFile(configuration.getTitle(), c);
        launch(CommandBuilder.of().add("-c").addFile(file.toString()));
    }

    @Override
    public boolean supportsPasswordPassing() {
        return false;
    }

    @Override
    public String getWebsite() {
        return "https://remmina.org/";
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
    public String getId() {
        return "app.remmina";
    }
}
