package io.xpipe.app.util;

import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreSecret;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.util.InPlaceSecretValue;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.None.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.InPlace.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.Prompt.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.CustomCommand.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.PasswordManager.class)
})
public interface SecretRetrievalStrategy {

    SecretQuery query();

    default boolean expectsQuery() {
        return true;
    }

    @JsonTypeName("none")
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
    @Jacksonized
    class InPlace implements SecretRetrievalStrategy {

        DataStoreSecret value;

        public InPlace(DataStoreSecret value) {
            this.value = value;
        }

        @Override
        public SecretQuery query() {
            return new SecretQuery() {
                @Override
                public SecretQueryResult query(String prompt) {
                    return new SecretQueryResult(
                            value != null ? value.getInternalSecret() : InPlaceSecretValue.of(""), SecretQueryState.NORMAL);
                }

                @Override
                public boolean cache() {
                    return false;
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
    class Prompt implements SecretRetrievalStrategy {

        @Override
        public SecretQuery query() {
            return new SecretQuery() {
                @Override
                public SecretQueryResult query(String prompt) {
                    return AskpassAlert.queryRaw(prompt, null);
                }

                @Override
                public boolean cache() {
                    return true;
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
        public SecretQuery query() {
            return new SecretQuery() {
                @Override
                public SecretQueryResult query(String prompt) {
                    var cmd = AppPrefs.get().passwordManagerString(key);
                    if (cmd == null) {
                        return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                    }

                    String r;
                    try (var cc = new LocalStore().control().command(cmd).start()) {
                        r = cc.readStdoutOrThrow();
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable("Unable to retrieve password with command " + cmd, ex)
                                .handle();
                        return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                    }

                    if (r.lines().count() > 1 || r.isBlank()) {
                        throw ErrorEvent.expected(
                                new IllegalArgumentException("Received not exactly one output line:\n" + r + "\n\n"
                                        + "XPipe requires your password manager command to output only the raw password."
                                        + " If the output includes any formatting, messages, or your password key either matched multiple entries or none,"
                                        + " you will have to change the command and/or password key."));
                    }

                    return new SecretQueryResult(InPlaceSecretValue.of(r), SecretQueryState.NORMAL);
                }

                @Override
                public boolean cache() {
                    return false;
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
        public SecretQuery query() {
            return new SecretQuery() {
                @Override
                public SecretQueryResult query(String prompt) {
                    try (var cc = new LocalStore().control().command(command).start()) {
                        return new SecretQueryResult(InPlaceSecretValue.of(cc.readStdoutOrThrow()), SecretQueryState.NORMAL);
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable("Unable to retrieve password with command " + command, ex)
                                .handle();
                        return new SecretQueryResult(null, SecretQueryState.RETRIEVAL_FAILURE);
                    }
                }

                @Override
                public boolean cache() {
                    return false;
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
