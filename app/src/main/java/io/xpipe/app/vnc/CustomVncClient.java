package io.xpipe.app.vnc;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.Comp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.core.AppFontSizes;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.ExternalApplicationHelper;
import io.xpipe.app.pwman.PasswordManager;
import io.xpipe.app.pwman.PasswordManagerCommandTemplate;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.BindingsHelper;
import io.xpipe.app.util.LocalShell;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.core.process.CommandBuilder;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellScript;
import io.xpipe.core.util.InPlaceSecretValue;
import io.xpipe.core.util.SecretValue;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
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
                .addComp(new TextFieldComp(command, false)
                        .apply(struc -> struc.get().setPromptText("myvncclient $ADDRESS"))
                        .maxWidth(600), command)
                .bind(() -> CustomVncClient.builder().command(command.get()).build(), property);
    }

    String command;

    @Override
    public void launch(LaunchConfiguration configuration) throws Exception {
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
