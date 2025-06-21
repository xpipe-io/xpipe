package io.xpipe.app.vnc;

import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.CommandBuilder;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Locale;

@JsonTypeName("custom")
@Value
@Jacksonized
@Builder
public class CustomVncClient implements ExternalVncClient {

    static OptionsBuilder createOptions(Property<CustomVncClient> property) {
        var command = new SimpleObjectProperty<>(property.getValue().getCommand());
        return new OptionsBuilder()
                .nameAndDescription("customVncCommand")
                .addComp(
                        new TextFieldComp(command, false)
                                .apply(struc -> struc.get().setPromptText("myvncclient $ADDRESS"))
                                .maxWidth(600),
                        command)
                .bind(() -> CustomVncClient.builder().command(command.get()).build(), property);
    }

    String command;

    @Override
    public void launch(VncLaunchConfig configuration) throws Exception {
        if (command == null) {
            return;
        }

        var address = configuration.getHost() + ":" + configuration.getPort();
        var format = command.toLowerCase(Locale.ROOT).contains("$address") ? command : command + " $ADDRESS";
        var toExecute = ExternalApplicationHelper.replaceVariableArgument(format, "ADDRESS", address);
        ExternalApplicationHelper.startAsync(CommandBuilder.of().add(toExecute));
    }

    @Override
    public boolean supportsPasswords() {
        return false;
    }
}
