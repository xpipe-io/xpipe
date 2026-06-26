package io.xpipe.app.vnc;

import io.xpipe.app.comp.base.TextAreaComp;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.rdp.RemminaRdpClient;
import io.xpipe.app.util.LocalFileTracker;
import io.xpipe.app.util.RemminaHelper;

import com.fasterxml.jackson.annotation.JsonTypeName;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleStringProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

@Builder
@Jacksonized
@JsonTypeName("remmina")
@Value
public class RemminaVncClient implements ExternalApplicationType.LinuxApplication, ExternalVncClient {

    String options;

    @SuppressWarnings("unused")
    static OptionsBuilder createOptions(Property<RemminaVncClient> property) {
        var options = new SimpleStringProperty(property.getValue().getOptions());

        return new OptionsBuilder()
                .nameAndDescription("remminaVncArguments")
                .documentationLink("https://gitlab.com/Remmina/Remmina/-/wikis/Remmina-Config-File-Options")
                .addComp(new TextAreaComp(options).applyStructure(structure -> {
                    structure.getTextArea().setPromptText(
                            """
                            showcursor=1
                            disableserverinput=1
                            tightencoding=1
                            """);
                }).maxWidth(600), options)
                .bind(
                        () -> RemminaVncClient.builder().options(options.get()).build(),
                        property);
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
        var file = RemminaHelper.writeRemminaVncConfigFile(configuration, encrypted.orElse(null), options);
        launch(CommandBuilder.of().add("-c").addFile(file.toString()));
        LocalFileTracker.deleteOnExit(file);
    }

    @Override
    public boolean supportsPasswords() {
        return true;
    }

    @Override
    public String getWebsite() {
        return "https://remmina.org/";
    }

    @Override
    public String getFlatpakId() {
        return "org.remmina.Remmina";
    }
}
