package io.xpipe.core.util;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Random;

@JsonTypeName("default")
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class DefaultSecretValue extends AesSecretValue {

    public DefaultSecretValue(char[] secret) {
        super(secret);
    }

    @Override
    public SecretValue inPlace() {
        return this;
    }

    protected SecretKey getAESKey(int keysize) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        var salt = new byte[16];
        new Random(keysize).nextBytes(salt);
        KeySpec spec = new PBEKeySpec(new char[]{
                'X', 'P', 'E' << 1
        }, salt, 2048, keysize);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}
