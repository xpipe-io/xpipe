package io.xpipe.app.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.SecretValue;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.function.Supplier;

@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "type")
@JsonSubTypes({
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.None.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.Reference.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.InPlace.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.Prompt.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.CustomCommand.class),
    @JsonSubTypes.Type(value = SecretRetrievalStrategy.PasswordManager.class)
})
public interface SecretRetrievalStrategy {

    SecretValue retrieve(String displayName, DataStore store) throws Exception;

    boolean supportsLocalAskpass();

    @JsonTypeName("none")
    public static class None implements SecretRetrievalStrategy {

        @Override
        public SecretValue retrieve(String displayName, DataStore store) {
            return null;
        }

        @Override
        public boolean supportsLocalAskpass() {
            return true;
        }
    }

    @JsonTypeName("reference")
    public static class Reference implements SecretRetrievalStrategy {

        @JsonIgnore
        private final Supplier<SecretValue> supplier;

        public Reference(Supplier<SecretValue> supplier) {
            this.supplier = supplier;
        }

        @Override
        public SecretValue retrieve(String displayName, DataStore store) {
            return supplier.get();
        }

        @Override
        public boolean supportsLocalAskpass() {
            return false;
        }
    }

    @JsonTypeName("inPlace")
    @Getter
    @Builder
    @Value
    @Jacksonized
    public static class InPlace implements SecretRetrievalStrategy {

        SecretValue value;

        public InPlace(SecretValue value) {
            this.value = value;
        }

        @Override
        public SecretValue retrieve(String displayName, DataStore store) {
            return value;
        }

        @Override
        public boolean supportsLocalAskpass() {
            return false;
        }
    }

    @JsonTypeName("prompt")
    public static class Prompt implements SecretRetrievalStrategy {

        @Override
        public SecretValue retrieve(String displayName, DataStore store) {
            return AskpassAlert.query(displayName, store);
        }

        @Override
        public boolean supportsLocalAskpass() {
            return true;
        }
    }

    @JsonTypeName("passwordManager")
    @Builder
    @Jacksonized
    @Value
    public static class PasswordManager implements SecretRetrievalStrategy {

        String key;

        @Override
        public SecretValue retrieve(String displayName, DataStore store) throws Exception {
            var cmd = AppPrefs.get().passwordManagerString(key);
            if (cmd == null) {
                return null;
            }

            try (var cc = new LocalStore().createBasicControl().command(cmd).start()) {
                return SecretHelper.encrypt(cc.readStdoutOrThrow());
            }
        }

        @Override
        public boolean supportsLocalAskpass() {
            return false;
        }
    }

    @JsonTypeName("customCommand")
    @Builder
    @Jacksonized
    @Value
    public static class CustomCommand implements SecretRetrievalStrategy {

        String command;

        @Override
        public SecretValue retrieve(String displayName, DataStore store) throws Exception {
            try (var cc = new LocalStore().createBasicControl().command(command).start()) {
                return SecretHelper.encrypt(cc.readStdoutOrThrow());
            }
        }

        @Override
        public boolean supportsLocalAskpass() {
            return false;
        }
    }
}
