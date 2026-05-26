package io.xpipe.app.secret;

import io.xpipe.app.comp.base.IntegratedTextAreaComp;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.platform.OptionsBuilder;
import io.xpipe.app.process.LocalShell;
import io.xpipe.app.process.ShellScript;
import io.xpipe.app.util.Validators;
import io.xpipe.core.InPlaceSecretValue;

import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;

@JsonTypeName("customCommand")
@Builder
@Jacksonized
@Value
public class SecretCustomCommandStrategy implements SecretRetrievalStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<SecretCustomCommandStrategy> p, SecretStrategyChoiceConfig config) {
        var options = new OptionsBuilder();
        var cmdProperty = options.map(p, SecretCustomCommandStrategy::getCommand);
        return options.nameAndDescription("customCommandValue")
                .addComp(
                        IntegratedTextAreaComp.script(
                                cmdProperty,
                                new ReadOnlyObjectWrapper<>(
                                        LocalShell.getDialect().getScriptFileEnding()),
                                true),
                        cmdProperty)
                .nonNull()
                .bind(
                        () -> {
                            return new SecretCustomCommandStrategy(cmdProperty.getValue());
                        },
                        p);
    }

    ShellScript command;

    @Override
    public void checkComplete() throws ValidationException {
        Validators.nonNull(command);
    }

    @Override
    public SecretQuery query() {
        return new SecretQuery() {
            @Override
            public SecretQueryResult query(String prompt, boolean forceFocus) {
                if (command == null || command.getValue().isBlank()) {
                    throw ErrorEventFactory.expected(new IllegalStateException("No custom command specified"));
                }

                try (var sc = ProcessControlProvider.get()
                        .createLocalProcessControl(true)
                        .start()) {
                    var cc = sc.command(command);
                    return new SecretQueryResult(
                            InPlaceSecretValue.of(cc.readStdoutOrThrow()), SecretQueryState.NORMAL);
                } catch (Exception ex) {
                    ErrorEventFactory.fromThrowable("Unable to retrieve password with command " + command, ex)
                            .handle();
                    return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                }
            }

            @Override
            public Duration cacheDuration() {
                return Duration.ZERO;
            }

            @Override
            public boolean retryOnFail() {
                return false;
            }

            @Override
            public boolean requiresUserInteraction() {
                return false;
            }
        };
    }
}
