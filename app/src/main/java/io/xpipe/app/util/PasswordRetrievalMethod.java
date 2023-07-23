package io.xpipe.app.util;

import io.xpipe.core.impl.LocalStore;
import io.xpipe.core.util.SecretValue;

import java.util.function.Supplier;

public abstract class PasswordRetrievalMethod {

    public abstract SecretValue retrieve(String displayName) throws Exception;

    public static class None extends PasswordRetrievalMethod {

        @Override
        public SecretValue retrieve(String displayName) {
            return null;
        }
    }

    public static class Unsupported extends PasswordRetrievalMethod {

        @Override
        public SecretValue retrieve(String displayName) {
            throw new UnsupportedOperationException();
        }
    }

    public static class Reference extends PasswordRetrievalMethod {

        private final Supplier<SecretValue> supplier;

        public Reference(Supplier<SecretValue> supplier) {
            this.supplier = supplier;
        }

        @Override
        public SecretValue retrieve(String displayName) {
            return supplier.get();
        }
    }

    public static class Prompt extends PasswordRetrievalMethod {

        @Override
        public SecretValue retrieve(String displayName) {
            return AskpassAlert.query(displayName);
        }
    }

    public static class Command extends PasswordRetrievalMethod {

        String command;

        @Override
        public SecretValue retrieve(String displayName) throws Exception {
            try (var cc = new LocalStore().createBasicControl().command(command).start()) {
                var read = cc.readStdoutDiscardErr();
                return SecretHelper.encrypt(read);
            }
        }
    }

    public static class KeePass extends PasswordRetrievalMethod {

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
