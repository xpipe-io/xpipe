package io.xpipe.app.util;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStorageUserHandler;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import lombok.extern.jackson.Jacksonized;

import javax.crypto.SecretKey;

@EqualsAndHashCode
@Builder
@Jacksonized
@ToString
public class EncryptionToken {

    private static EncryptionToken userToken;
    private static EncryptionToken vaultToken;

    private static EncryptionToken createUserToken() {
        var userHandler = DataStorageUserHandler.getInstance();
        if (userHandler.getActiveUser() == null) {
            throw new IllegalStateException("No active user available");
        }

        var userSecretValue =
                new PasswordLockSecretValue(userHandler.getActiveUser().toCharArray()) {
                    @Override
                    protected SecretKey getSecretKey() {
                        return userHandler.getEncryptionKey();
                    }
                };
        var userCrypt = userSecretValue.getEncryptedValue();
        return EncryptionToken.builder().token(userCrypt).build();
    }

    private static EncryptionToken createVaultToken() {
        var secretValue = new VaultKeySecretValue(new char[] {'x', 'p', 'i', 'p', 'e'});
        var crypt = secretValue.getEncryptedValue();
        return EncryptionToken.builder().token(crypt).build();
    }

    public static EncryptionToken ofInvalid() {
        return EncryptionToken.builder().token("").isUser(false).isVault(false).build();
    }

    public static EncryptionToken ofUser() {
        if (userToken == null) {
            userToken = createUserToken();
        }
        return userToken;
    }

    public static EncryptionToken ofVaultKey() {
        if (vaultToken == null) {
            vaultToken = createVaultToken();
        }
        return vaultToken;
    }

    private final String token;

    @JsonIgnore
    private Boolean isUser;

    @JsonIgnore
    private Boolean isVault;

    public boolean canDecrypt() {
        return isVault() || isUser();
    }

    public String decode(SecretKey secretKey) {
        var secretValue = new PasswordLockSecretValue(token) {
            @Override
            protected SecretKey getSecretKey() {
                return secretKey;
            }
        };
        return secretValue.getSecretValue();
    }

    public boolean isUser() {
        if (isUser != null) {
            return isUser;
        }

        var userHandler = DataStorageUserHandler.getInstance();
        if (userHandler.getActiveUser() == null) {
            return (isUser = false);
        }

        return (isUser = userHandler.getActiveUser().equals(decode(userHandler.getEncryptionKey())));
    }

    public boolean isVault() {
        if (isVault != null) {
            return isVault;
        }

        var key = DataStorage.get().getVaultKey();
        var s = decode(key);
        return (isVault = s.equals("xpipe"));
    }
}
