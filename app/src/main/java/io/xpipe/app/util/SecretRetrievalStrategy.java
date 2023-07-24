package io.xpipe.app.util;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.util.SecretValue;
import lombok.Getter;

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
public abstract class SecretRetrievalStrategy {

    public abstract SecretValue retrieve(String displayName) throws Exception;

    @JsonTypeName("none")
    public static class None extends SecretRetrievalStrategy {

        @Override
        public SecretValue retrieve(String displayName) {
            return null;
        }
    }

    @JsonTypeName("unsupported")
    public static class Unsupported extends SecretRetrievalStrategy {

        @Override
        public SecretValue retrieve(String displayName) {
            throw new UnsupportedOperationException();
        }
    }

    @JsonTypeName("reference")
    public static class Reference extends SecretRetrievalStrategy {

        @JsonIgnore
        private final Supplier<SecretValue> supplier;

        public Reference(Supplier<SecretValue> supplier) {
            this.supplier = supplier;
        }

        @Override
        public SecretValue retrieve(String displayName) {
            return supplier.get();
        }
    }

    @JsonTypeName("inPlace")
    @Getter
    public static class InPlace extends SecretRetrievalStrategy {

        private final SecretValue value;

        public InPlace(SecretValue value) {
            this.value = value;
        }

        @Override
        public SecretValue retrieve(String displayName) {
            return value;
        }
    }

    @JsonTypeName("prompt")
    public static class Prompt extends SecretRetrievalStrategy {

        @Override
        public SecretValue retrieve(String displayName) {
            return AskpassAlert.query(displayName);
        }
    }

    @JsonTypeName("command")
    public static class Command extends SecretRetrievalStrategy {

        String command;

        @Override
        public SecretValue retrieve(String displayName) throws Exception {
            try (var cc = new LocalStore().createBasicControl().command(command).start()) {
                var read = cc.readStdoutDiscardErr();
                return SecretHelper.encrypt(read);
            }
        }
    }

    @JsonTypeName("keepass")
    public static class KeePass extends SecretRetrievalStrategy {

        String command;

        @Override
        public SecretValue retrieve(String displayName) throws Exception {
            try (var cc = new LocalStore().createBasicControl().command(command).start()) {
                var read = cc.readStdoutDiscardErr();
                return SecretHelper.encrypt(read);
            }
        }
    }
}
