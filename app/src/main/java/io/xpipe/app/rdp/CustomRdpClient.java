package io.xpipe.app.rdp;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.prefs.ExternalApplicationType;
import io.xpipe.app.process.CommandBuilder;
import io.xpipe.app.vnc.CustomVncClient;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Locale;

@JsonTypeName("custom")
@Value
@Jacksonized
@Builder
public class CustomRdpClient implements ExternalApplicationType, ExternalRdpClient {

    @SuppressWarnings("unused")
    static OptionsBuilder createOptions(Property<CustomRdpClient> property) {
        var command = new SimpleObjectProperty<>(property.getValue().getCommand());
        return new OptionsBuilder()
                .nameAndDescription("customRdpClientCommand")
                .addComp(
                        new TextFieldComp(command, false)
                                .apply(struc -> struc.get().setPromptText("myrdpclient -c $FILE"))
                                .maxWidth(600),
                        command)
                .bind(() -> CustomRdpClient.builder().command(command.get()).build(), property);
    }

    String command;

    @Override
    public void launch(RdpLaunchConfig configuration) throws Exception {
        if (command == null || command.isBlank()) {
            throw ErrorEventFactory.expected(new IllegalStateException("No custom RDP command specified"));
        }

        var format =
                command.toLowerCase(Locale.ROOT).contains("$file") ? command : command + " $FILE";
        ExternalApplicationHelper.startAsync(CommandBuilder.of()
                .add(ExternalApplicationHelper.replaceVariableArgument(
                        format,
                        "FILE",
                        writeRdpConfigFile(configuration.getTitle(), configuration.getConfig())
                                .toString())));
    }

    @Override
    public boolean supportsPasswordPassing() {
        return false;
    }

    @Override
    public String getWebsite() {
        return null;
    }

    @Override
    public boolean isAvailable() {
        return true;
    }

    @Override
    public String getId() {
        return "app.custom";
    }
}
