package io.xpipe.app.storage;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.xpipe.app.secret.EncryptionToken;

public enum DataStoreScope {

    @JsonProperty("vault")
    VAULT() {
        @Override
        public EncryptionToken getToken() {
            return EncryptionToken.ofVaultKey();
        }

        @Override
        public String getId() {
            return "vault";
        }
    },
    @JsonProperty("group")
    GROUP() {
        @Override
        public EncryptionToken getToken() {
            return EncryptionToken.ofVaultKey();
        }

        @Override
        public String getId() {
            return "group";
        }
    },
    @JsonProperty("user")
    USER() {
        @Override
        public EncryptionToken getToken() {
            return EncryptionToken.ofVaultKey();
        }

        @Override
        public String getId() {
            return "user";
        }
    };

    public abstract String getId();

    public abstract EncryptionToken getToken();
}
