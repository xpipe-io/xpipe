package io.xpipe.app.secret;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.comp.base.ContextualFileReferenceChoiceComp;
import io.xpipe.app.comp.base.TextFieldComp;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.util.OptionsBuilder;
import io.xpipe.app.util.Validators;
import io.xpipe.core.InPlaceSecretValue;
import javafx.beans.property.Property;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.SimpleObjectProperty;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.time.Duration;
import java.util.List;

@JsonTypeName("customCommand")
@Builder
@Jacksonized
@Value
public class SecretCustomCommandStrategy implements SecretRetrievalStrategy {

    @SuppressWarnings("unused")
    public static OptionsBuilder createOptions(
            Property<SecretCustomCommandStrategy> p, SecretStrategyChoiceConfig config) {
        var cmdProperty = new SimpleObjectProperty<>(p.getValue() != null ? p.getValue().getCommand() : null);
        var content = new TextFieldComp(cmdProperty);
        return new OptionsBuilder()
                .addComp(content, cmdProperty)
                .nonNull()
                .bind(
                        () -> {
                            return new SecretCustomCommandStrategy(cmdProperty.getValue());
                        },
                        p);
    }

    String command;

    @Override
    public void checkComplete() throws ValidationException {
        Validators.nonNull(command);
    }

    @Override
    public SecretQuery query() {
        return new SecretQuery() {
            @Override
            public SecretQueryResult query(String prompt) {
                if (command == null || command.isBlank()) {
                    throw ErrorEventFactory.expected(new IllegalStateException("No custom command specified"));
                }

                try (var cc = ProcessControlProvider.get().createLocalProcessControl(true).command(command).start()) {
                    return new SecretQueryResult(InPlaceSecretValue.of(cc.readStdoutOrThrow()), SecretQueryState.NORMAL);
                } catch (Exception ex) {
                    ErrorEventFactory.fromThrowable("Unable to retrieve password with command " + command, ex).handle();
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
