package io.xpipe.app.util;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.AesSecretValue;
import io.xpipe.core.InPlaceSecretValue;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import javax.crypto.SecretKey;

@JsonTypeName("vault")
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class VaultKeySecretValue extends AesSecretValue {

    public VaultKeySecretValue(String encryptedValue) {
        super(encryptedValue);
    }

    public VaultKeySecretValue(char[] secret) {
        super(secret);
    }

    @Override
    protected SecretKey getSecretKey() {
        return DataStorage.get() != null ? DataStorage.get().getVaultKey() : null;
    }

    @Override
    public InPlaceSecretValue inPlace() {
        return new InPlaceSecretValue(getSecret());
    }

    @Override
    public String toString() {
        return "<vault secret>";
    }
}
