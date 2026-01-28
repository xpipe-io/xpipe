package io.xpipe.app.spice;

import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.process.CommandBuilder;

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
public class CustomSpiceClient implements ExternalSpiceClient {

    String command;

    @SuppressWarnings("unused")
    static OptionsBuilder createOptions(Property<CustomSpiceClient> property) {
        var command = new SimpleObjectProperty<>(property.getValue().getCommand());
        return new OptionsBuilder()
                .nameAndDescription("customSpiceCommand")
                .addComp(
                        new TextFieldComp(command, false)
                                .apply(struc -> struc.setPromptText("myspiceClient $FILE"))
                                .maxWidth(600),
                        command)
                .bind(() -> CustomSpiceClient.builder().command(command.get()).build(), property);
    }

    @Override
    public void launch(SpiceLaunchConfig configuration) throws Exception {
        if (command == null) {
            return;
        }

        var format = command.toLowerCase(Locale.ROOT).contains("$file") ? command : command + " $FILE";
        var toExecute = ExternalApplicationHelper.replaceVariableArgument(
                format, "ADDRESS", configuration.getFile().toString());
        ExternalApplicationHelper.startAsync(CommandBuilder.of().add(toExecute));
    }

    @Override
    public String getWebsite() {
        return null;
    }
}
