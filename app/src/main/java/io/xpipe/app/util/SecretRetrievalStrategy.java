package io.xpipe.app.util;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreSecret;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.util.InPlaceSecretValue;
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
                    return new SecretQueryResult(value != null ? value.getInternalSecret() : InPlaceSecretValue.of(""), false);
                }

                @Override
                public boolean cache() {
                    return false;
                }

                @Override
                public boolean retryOnFail() {
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
                    return AskpassAlert.queryRaw(prompt);
                }

                @Override
                public boolean cache() {
                    return true;
                }

                @Override
                public boolean retryOnFail() {
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
                        return null;
                    }

                    try (var cc = new LocalStore().control().command(cmd).start()) {
                        return new SecretQueryResult(InPlaceSecretValue.of(cc.readStdoutOrThrow()), false);
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable("Unable to retrieve password with command " + cmd, ex).handle();
                        return new SecretQueryResult(null, true);
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
                        return new SecretQueryResult(InPlaceSecretValue.of(cc.readStdoutOrThrow()), false);
                    } catch (Exception ex) {
                        ErrorEvent.fromThrowable("Unable to retrieve password with command " + command, ex).handle();
                        return new SecretQueryResult(null, true);
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
            };
        }
    }
}
