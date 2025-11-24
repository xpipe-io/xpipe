package io.xpipe.app.storage;

import io.xpipe.app.secret.EncryptionToken;

public enum DataStoreScope {

    VAULT() {
        @Override
        public EncryptionToken getToken() {
            return EncryptionToken.ofVaultKey();
        }
    },
    GROUP() {
        @Override
        public EncryptionToken getToken() {
            return EncryptionToken.ofVaultKey();
        }
    },
    USER() {
        @Override
        public EncryptionToken getToken() {
            return EncryptionToken.ofVaultKey();
        }
    };

    public abstract EncryptionToken getToken();
}
