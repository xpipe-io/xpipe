package io.xpipe.app.util;

import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ValidationException;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.InPlaceSecretValue;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.nio.CharBuffer;
import java.time.Duration;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.None.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.InPlace.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.Prompt.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.CustomCommand.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.PasswordManager.class)
})
public interface SecretRetrievalStrategy {

    default void checkComplete() throws ValidationException {}

    SecretQuery query();

    default boolean expectsQuery() {
        return true;
    }

    @JsonTypeName("none")
    @Value
    class None implements SecretRetrievalStrategy {

        @Override
        public SecretQuery query() {
            return null;
        }

        public boolean expectsQuery() {
            return false;
        }
    }

    @JsonTypeName("inPlace")
    @Builder
    @Value
    class InPlace implements SecretRetrievalStrategy {

        InPlaceSecretValue value;

        public InPlace(InPlaceSecretValue value) {
            this.value = value;
        }

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(value);
        }

        @Override
        public SecretQuery query() {
            return new SecretQuery() {
                @Override
                public SecretQueryResult query(String prompt) {
                    return new SecretQueryResult(value, SecretQueryState.NORMAL);
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

    @JsonTypeName("prompt")
    @Value
    class Prompt implements SecretRetrievalStrategy {

        @Override
        public SecretQuery query() {
            return new SecretQuery() {
                @Override
                public SecretQueryResult query(String prompt) {
                    return AskpassAlert.queryRaw(prompt, null, true);
                }

                @Override
                public Duration cacheDuration() {
                    return null;
                }

                @Override
                public boolean retryOnFail() {
                    return true;
                }

                @Override
                public boolean requiresUserInteraction() {
                    return true;
                }
            };
        }
    }

    @JsonTypeName("passwordManager")
    @Builder
    @Jacksonized
    @Value
    class PasswordManager implements SecretRetrievalStrategy {

        String key;

        @Override
        public void checkComplete() throws ValidationException {
            Validators.nonNull(key);
        }

        @Override
        public SecretQuery query() {
            return new SecretQuery() {
                @Override
                public SecretQueryResult query(String prompt) {
                    var pm = AppPrefs.get().passwordManager().getValue();
                    if (pm == null) {
                        ErrorEventFactory.fromMessage(
                                        "A password manager was requested but no password manager has been set in the settings menu")
                                .expected()
                                .handle();
                        return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                    }

                    var r = pm.retrieveCredentials(key);
                    if (r == null || r.getPassword() == null) {
                        return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                    }

                    r.getPassword().withSecretValue(chars -> {
                        var seq = CharBuffer.wrap(chars);
                        var newline = seq.chars().anyMatch(value -> value == 10);
                        if (seq.length() == 0 || newline) {
                            throw ErrorEventFactory.expected(
                                    new IllegalArgumentException("Received not exactly one output line:\n" + r
                                            + "\n\n"
                                            + "XPipe requires your password manager command to output only the raw password."
                                            + " If the output includes any formatting, messages, or your password key either matched multiple entries or "
                                            + "none,"
                                            + " you will have to change the command and/or password key."));
                        }
                    });
                    return new SecretQueryResult(r.getPassword(), SecretQueryState.NORMAL);
                }

                @Override
                public Duration cacheDuration() {
                    // To reduce password manager access, cache it for a few seconds
                    return Duration.ofSeconds(10);
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

    @JsonTypeName("customCommand")
    @Builder
    @Jacksonized
    @Value
    class CustomCommand implements SecretRetrievalStrategy {

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

                    try (var cc = ProcessControlProvider.get()
                            .createLocalProcessControl(true)
                            .command(command)
                            .start()) {
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
}
