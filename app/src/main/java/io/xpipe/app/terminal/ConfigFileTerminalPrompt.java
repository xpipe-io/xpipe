package io.xpipe.app.terminal;

import io.xpipe.app.comp.base.ButtonComp;
import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.core.AppI18n;
import io.xpipe.app.password.KeePassXcAssociationKey;
import io.xpipe.app.password.KeePassXcManager;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.ThreadHelper;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.process.ShellTerminalInitCommand;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.experimental.SuperBuilder;

import java.util.function.Function;

@SuperBuilder
public abstract class ConfigFileTerminalPrompt implements TerminalPrompt {

    protected static OptionsBuilder createOptions(Property<ConfigFileTerminalPrompt> p, String extension, Function<String, ConfigFileTerminalPrompt> creator) {
        var prop = new SimpleObjectProperty<String>();
        return new OptionsBuilder()
                .nameAndDescription("configuration")
                .addComp(new IntegratedTextAreaComp(prop, false, "config", new SimpleStringProperty(extension)), prop)
                .bind(
                        () -> {
                            return creator.apply(prop.getValue());
                        },
                        p);
    }

    protected String configuration;
}
