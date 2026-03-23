package io.xpipe.app.cred;

import io.xpipe.core.FailableSupplier;

import lombok.EqualsAndHashCode;
import lombok.ToString;

import java.util.Optional;

public interface PublicKeyStrategy {

    String retrievePublicKey() throws Exception;

    @EqualsAndHashCode
    @ToString
    final class Fixed implements PublicKeyStrategy {

        public static Fixed of(String publicKey) {
            return publicKey != null ? new Fixed(publicKey) : null;
        }

        private final String publicKey;

        public Fixed(String publicKey) {
            this.publicKey = publicKey;
        }

        public String get() {
            return publicKey;
        }

        private Optional<String> getFixedPublicKey() {
            return Optional.ofNullable(publicKey);
        }

        @Override
        public String retrievePublicKey() {
            return getFixedPublicKey().orElseThrow();
        }
    }

    final class Dynamic implements PublicKeyStrategy {

        private final FailableSupplier<String> publicKey;

        public Dynamic(FailableSupplier<String> publicKey) {
            this.publicKey = publicKey;
        }

        @Override
        public int hashCode() {
            return getClass().hashCode();
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof Dynamic;
        }

        @Override
        public String toString() {
            return "<dynamic>";
        }

        @Override
        public String retrievePublicKey() throws Exception {
            var r = publicKey.get();
            return r;
        }
    }
}
