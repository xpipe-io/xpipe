package io.xpipe.app.rdp;

import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefsCategory;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.util.*;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeName("remmina")
@Value
@Jacksonized
@Builder
public class RemminaRdpClient implements ExternalApplicationType.LinuxApplication, ExternalRdpClient {

    String options;

    @SuppressWarnings("unused")
    static OptionsBuilder createOptions(Property<RemminaRdpClient> property) {
        var options = new SimpleStringProperty(property.getValue().getOptions());

        return new OptionsBuilder()
                .nameAndDescription("remminaRdpArguments")
                .documentationLink("https://gitlab.com/Remmina/Remmina/-/wikis/Remmina-Config-File-Options")
                .addComp(new TextAreaComp(options).applyStructure(structure -> {
                    structure.getTextArea().setPromptText(
                            """
                            websockets=1
                            timeout=1000
                            restricted-admin=1
                            """);
                }).maxWidth(600), options)
                .bind(
                        () -> RemminaRdpClient.builder().options(options.get()).build(),
                        property);
    }

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        // Remmina does not support RemoteApps
        if (configuration.isRemoteApp()) {
            var freerdp = new FreeRdpClient(null);
            if (freerdp.isAvailable()) {
                freerdp.launch(configuration);
                return;
            }
        }

        var file = RemminaHelper.writeRemminaRdpConfigFile(configuration, options);
        LocalFileTracker.deleteOnExit(file);
        launch(CommandBuilder.of().add("-c").addFile(file.toString()));
    }

    @Override
    public boolean supportsPasswordPassing() {
        return true;
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
    public String getFlatpakId() {
        return "org.remmina.Remmina";
    }
}
