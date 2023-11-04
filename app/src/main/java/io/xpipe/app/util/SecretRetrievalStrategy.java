package io.xpipe.app.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.issue.ErrorEvent;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.ProcessOutputException;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.util.SecretValue;
import lombok.Builder;
import lombok.Getter;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.UUID;
import java.util.function.Supplier;

@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes(
        {
                @JsonSubTypes.Type(value = SecretRetrievalStrategy.None.class), @JsonSubTypes.Type(value = SecretRetrievalStrategy.Reference.class),
                @JsonSubTypes.Type(value = SecretRetrievalStrategy.InPlace.class), @JsonSubTypes.Type(value = SecretRetrievalStrategy.Prompt.class),
                @JsonSubTypes.Type(value = SecretRetrievalStrategy.CustomCommand.class),
                @JsonSubTypes.Type(value = SecretRetrievalStrategy.PasswordManager.class)
        })
public interface SecretRetrievalStrategy {

    SecretValue retrieve(String displayName, UUID id, int sub) throws Exception;

    boolean isLocalAskpassCompatible();

    boolean shouldCache();

    @JsonTypeName("none")
    class None implements SecretRetrievalStrategy {

        @Override
        public SecretValue retrieve(String displayName, UUID id, int sub) {
            return null;
        }

        @Override
        public boolean isLocalAskpassCompatible() {
            return false;
        }

        @Override
        public boolean shouldCache() {
            return false;
        }
    }

    @JsonTypeName("reference")
    class Reference implements SecretRetrievalStrategy {

        @JsonIgnore
        private final Supplier<SecretValue> supplier;

        public Reference(Supplier<SecretValue> supplier) {
            this.supplier = supplier;
        }

        @Override
        public SecretValue retrieve(String displayName, UUID id, int sub) {
            return supplier.get();
        }

        @Override
        public boolean isLocalAskpassCompatible() {
            return false;
        }

        @Override
        public boolean shouldCache() {
            return false;
        }
    }

    @JsonTypeName("inPlace")
    @Getter
    @Builder
    @Value
    @Jacksonized
    class InPlace implements SecretRetrievalStrategy {

        SecretValue value;

        public InPlace(SecretValue value) {
            this.value = value;
        }

        @Override
        public SecretValue retrieve(String displayName, UUID id, int sub) {
            return value;
        }

        @Override
        public boolean isLocalAskpassCompatible() {
            return false;
        }

        @Override
        public boolean shouldCache() {
            return false;
        }
    }

    @JsonTypeName("prompt")
    class Prompt implements SecretRetrievalStrategy {

        @Override
        public SecretValue retrieve(String displayName, UUID id, int sub) {
            return AskpassAlert.query(displayName, UUID.randomUUID(), id, sub);
        }

        @Override
        public boolean isLocalAskpassCompatible() {
            return true;
        }

        @Override
        public boolean shouldCache() {
            return true;
        }
    }

    @JsonTypeName("dynamicPrompt")
    class DynamicPrompt implements SecretRetrievalStrategy {

        @Override
        public SecretValue retrieve(String displayName, UUID id, int sub) {
            return AskpassAlert.query(displayName, UUID.randomUUID(), id, sub);
        }

        @Override
        public boolean isLocalAskpassCompatible() {
            return true;
        }

        @Override
        public boolean shouldCache() {
            return false;
        }
    }

    @JsonTypeName("passwordManager")
    @Builder
    @Jacksonized
    @Value
    class PasswordManager implements SecretRetrievalStrategy {

        String key;

        @Override
        public SecretValue retrieve(String displayName, UUID id, int sub) throws Exception {
            var cmd = AppPrefs.get().passwordManagerString(key);
            if (cmd == null) {
                return null;
            }

            try (var cc = new LocalStore().control().command(cmd).start()) {
                return SecretHelper.encrypt(cc.readStdoutOrThrow());
            } catch (ProcessOutputException ex) {
                throw ErrorEvent.unreportable(ProcessOutputException.withPrefix("Unable to retrieve password with command " + cmd, ex));
            }
        }

        @Override
        public boolean isLocalAskpassCompatible() {
            return false;
        }

        @Override
        public boolean shouldCache() {
            return false;
        }
    }

    @JsonTypeName("customCommand")
    @Builder
    @Jacksonized
    @Value
    class CustomCommand implements SecretRetrievalStrategy {

        String command;

        @Override
        public SecretValue retrieve(String displayName, UUID id, int sub) throws Exception {
            try (var cc = new LocalStore().control().command(command).start()) {
                return SecretHelper.encrypt(cc.readStdoutOrThrow());
            }
        }

        @Override
        public boolean isLocalAskpassCompatible() {
            return false;
        }

        @Override
        public boolean shouldCache() {
            return false;
        }
    }
}
