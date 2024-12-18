package io.xpipe.app.storage;

import io.xpipe.app.util.EncryptionToken;
import io.xpipe.app.util.PasswordLockSecretValue;
import io.xpipe.app.util.VaultKeySecretValue;
import io.xpipe.core.util.JacksonMapper;
import io.xpipe.core.util.SecretValue;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Value;
import lombok.experimental.NonFinal;

import java.util.Arrays;

@Value
public class DataStorageSecret {

    public static DataStorageSecret ofCurrentSecret(SecretValue internalSecret) {
        var handler = DataStorageUserHandler.getInstance();
        return new DataStorageSecret(
                handler.getActiveUser() != null ? EncryptionToken.ofUser() : EncryptionToken.ofVaultKey(),
                null,
                internalSecret);
    }

    public static DataStorageSecret ofSecret(SecretValue internalSecret, EncryptionToken token) {
        return new DataStorageSecret(token, null, internalSecret);
    }

    @NonFinal
    JsonNode originalNode;

    SecretValue internalSecret;

    @NonFinal
    EncryptionToken encryptedToken;

    public DataStorageSecret(EncryptionToken encryptedToken, JsonNode originalNode, SecretValue internalSecret) {
        this.encryptedToken = encryptedToken;
        this.originalNode = originalNode;
        this.internalSecret = internalSecret;
    }

    public boolean requiresRewrite() {
        var isVault = encryptedToken.isVault();
        var isUser = encryptedToken.isUser();
        var userHandler = DataStorageUserHandler.getInstance();

        // User key must have changed
        if (!isUser && !isVault) {
            // There must be a key mismatch
            if (userHandler.getActiveUser() == null) {
                return false;
            }

            return true;
        }

        var hasUserKey = userHandler.getActiveUser() != null;
        // Switch from vault to user
        if (hasUserKey && isVault) {
            return true;
        }
        // Switch from user to vault
        if (!hasUserKey && isUser) {
            return true;
        }

        return false;
    }

    public JsonNode rewrite() {
        var handler = DataStorageUserHandler.getInstance();
        if (handler != null && handler.getActiveUser() != null && encryptedToken.isUser()) {
            var val = new PasswordLockSecretValue(getSecret());
            originalNode = JacksonMapper.getDefault().valueToTree(val);
            encryptedToken = EncryptionToken.ofUser();
            return originalNode;
        }

        var val = new VaultKeySecretValue(getSecret());
        originalNode = JacksonMapper.getDefault().valueToTree(val);
        encryptedToken = EncryptionToken.ofVaultKey();
        return originalNode;
    }

    public char[] getSecret() {
        return internalSecret != null ? internalSecret.getSecret() : new char[0];
    }

    @Override
    public int hashCode() {
        return Arrays.hashCode(getSecret());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof DataStorageSecret that)) {
            return false;
        }
        return Arrays.equals(getSecret(), that.getSecret());
    }
}
