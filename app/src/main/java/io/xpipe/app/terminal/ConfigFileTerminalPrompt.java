package io.xpipe.app.terminal;

import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.process.ShellControl;
import io.xpipe.app.process.ShellDialect;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.process.ShellTerminalInitCommand;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.core.FilePath;

import javafx.beans.property.Property;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;

import lombok.experimental.SuperBuilder;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;

@SuperBuilder
public abstract class ConfigFileTerminalPrompt implements TerminalPrompt {

    protected String configuration;

    protected static <T extends ConfigFileTerminalPrompt> OptionsBuilder createOptions(
            Property<T> p, Function<String, T> creator) {
        var prop = new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().configuration : null);
        return new OptionsBuilder()
                .nameAndDescription("terminalPromptConfig")
                .addComp(
                        new IntegratedTextAreaComp(
                                        prop,
                                        false,
                                        p.getValue() != null ? p.getValue().getId() : "config",
                                        new SimpleStringProperty(
                                                p.getValue() != null
                                                        ? p.getValue().getConfigFileExtension()
                                                        : null))
                                .prefHeight(400),
                        prop)
                .bind(
                        () -> {
                            return creator.apply(prop.getValue());
                        },
                        p);
    }

    @Override
    public ShellTerminalInitCommand terminalCommand() throws Exception {
        return new ShellTerminalInitCommand() {
            @Override
            public Optional<String> terminalContent(ShellControl shellControl) throws Exception {
                if (!installIfNeeded(shellControl)) {
                    return Optional.empty();
                }

                FilePath configFile = null;
                if (configuration != null && !configuration.isBlank()) {
                    configFile = shellControl
                            .view()
                            .writeTextFileDeterministic(getTargetConfigFile(shellControl), configuration);
                }

                var s = shellControl
                        .getShellDialect()
                        .addToPathVariableCommand(
                                List.of(getBinaryDirectory(shellControl).toString()), false);
                return Optional.of(s + "\n"
                        + setupTerminalCommand(shellControl, configFile).toString());
            }

            @Override
            public boolean canPotentiallyRunInDialect(ShellDialect dialect) {
                return getSupportedDialects().contains(dialect);
            }
        };
    }

    protected FilePath getTargetConfigFile(ShellControl shellControl) throws Exception {
        FilePath configFile = getConfigurationDirectory(shellControl).join(getId() + "." + getConfigFileExtension());
        return configFile;
    }

    protected abstract String getConfigFileExtension();

    protected abstract ShellScript setupTerminalCommand(ShellControl shellControl, FilePath config) throws Exception;
}
