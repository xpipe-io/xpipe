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
import io.xpipe.core.store.FilePath;
import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import lombok.experimental.SuperBuilder;

import java.util.function.Function;

@SuperBuilder
public abstract class ConfigFileTerminalPrompt implements TerminalPrompt {

    protected static <T extends ConfigFileTerminalPrompt> OptionsBuilder createOptions(Property<T> p, String extension, Function<String, T> creator) {
        var prop = new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().configuration : null);
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

    protected abstract FilePath prepareCustomConfigFile(ShellControl sc) throws Exception;

    protected abstract FilePath getDefaultConfigFile(ShellControl sc) throws Exception;

    @Override
    public ShellTerminalInitCommand terminalCommand(ShellControl sc) throws Exception {
        FilePath configFile;
        if (configuration == null || configuration.isBlank()) {
            configFile = getDefaultConfigFile(sc);
        } else {
            configFile = prepareCustomConfigFile(sc);
        }
        return terminalCommand(sc, configFile);
    }

    protected abstract ShellTerminalInitCommand terminalCommand(ShellControl shellControl, FilePath config) throws Exception;
}
