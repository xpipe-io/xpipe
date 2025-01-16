package io.xpipe.app.util;

import io.xpipe.app.storage.DataStorageUserHandler;
import io.xpipe.core.util.AesSecretValue;
import io.xpipe.core.util.InPlaceSecretValue;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import javax.crypto.SecretKey;

@JsonTypeName("locked")
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class PasswordLockSecretValue extends AesSecretValue {

    public PasswordLockSecretValue(String encryptedValue) {
        super(encryptedValue);
    }

    public PasswordLockSecretValue(char[] secret) {
        super(secret);
    }

    @Override
    protected SecretKey getSecretKey() {
        var handler = DataStorageUserHandler.getInstance();
        return handler != null ? handler.getEncryptionKey() : null;
    }

    @Override
    public InPlaceSecretValue inPlace() {
        return new InPlaceSecretValue(getSecret());
    }

    @Override
    public String toString() {
        return "<password lock secret>";
    }
}
