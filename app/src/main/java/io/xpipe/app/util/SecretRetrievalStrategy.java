package io.xpipe.app.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
        @JsonSubTypes.Type(value = SecretRetrievalStrategy.Unsupported.class),
        @JsonSubTypes.Type(value = SecretRetrievalStrategy.Reference.class),
        @JsonSubTypes.Type(value = SecretRetrievalStrategy.InPlace.class),
        @JsonSubTypes.Type(value = SecretRetrievalStrategy.Prompt.class),
        @JsonSubTypes.Type(value = SecretRetrievalStrategy.Command.class),
        @JsonSubTypes.Type(value = SecretRetrievalStrategy.KeePass.class)
})
public interface SecretRetrievalStrategy {

    SecretValue retrieve(String displayName, DataStore store) throws Exception;

    @JsonTypeName("none")
    public static class None implements SecretRetrievalStrategy {

        @Override
        public SecretValue retrieve(String displayName, DataStore store) {
            return null;
        }
    }

    @JsonTypeName("unsupported")
    public static class Unsupported implements SecretRetrievalStrategy {

        @Override
        public SecretValue retrieve(String displayName, DataStore store) {
            throw new UnsupportedOperationException();
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
    }

    @JsonTypeName("prompt")
    public static class Prompt implements SecretRetrievalStrategy {

        @Override
        public SecretValue retrieve(String displayName, DataStore store) {
            return AskpassAlert.query(displayName, store);
        }
    }

    @JsonTypeName("command")
    @Builder
    @Jacksonized
    @Value
    public static class Command implements SecretRetrievalStrategy {

        String command;

        @Override
        public SecretValue retrieve(String displayName, DataStore store) throws Exception {
            try (var cc = new LocalStore().createBasicControl().command(command).start()) {
                var read = cc.readStdoutDiscardErr();
                return SecretHelper.encrypt(read);
            }
        }
    }

    @JsonTypeName("keepass")
    @Builder
    @Jacksonized
    @Value
    public static class KeePass implements SecretRetrievalStrategy {

        String entry;

        @Override
        public SecretValue retrieve(String displayName, DataStore store) throws Exception {
            try (var cc = new LocalStore().createBasicControl().command(entry).start()) {
                var read = cc.readStdoutDiscardErr();
                return SecretHelper.encrypt(read);
            }
        }
    }
}
