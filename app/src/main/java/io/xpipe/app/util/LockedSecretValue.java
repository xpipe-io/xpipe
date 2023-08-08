package io.xpipe.app.util;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.AesSecretValue;
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

@JsonTypeName("locked")
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class LockedSecretValue extends AesSecretValue {

    public LockedSecretValue(char[] secret) {
        super(secret);
    }

    @Override
    public String toString() {
        return "<locked secret>";
    }

    protected SecretKey getAESKey(int keysize) throws NoSuchAlgorithmException, InvalidKeySpecException {
        var chars = AppPrefs.get().getLockPassword().getValue() != null
                ? AppPrefs.get().getLockPassword().getValue().getSecret()
                : new char[0];
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        var salt = new byte[16];
        new Random(keysize).nextBytes(salt);
        KeySpec spec = new PBEKeySpec(chars, salt, 8192, keysize);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}
