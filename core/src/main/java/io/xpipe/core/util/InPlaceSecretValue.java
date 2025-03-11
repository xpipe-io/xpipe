package io.xpipe.core.util;

import com.fasterxml.jackson.annotation.JsonTypeName;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Random;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

@JsonTypeName("default")
@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class InPlaceSecretValue extends AesSecretValue {

    private static final int AES_KEY_BIT = 128;
    private static final int SALT_BIT = 16;
    private static final int ITERATION_COUNT = 2048;
    private static final SecretKeyFactory SECRET_FACTORY;
    private static final SecretKey SECRET_KEY;

    static {
        try {
            SECRET_FACTORY = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");

            var salt = new byte[SALT_BIT];
            new Random(AES_KEY_BIT).nextBytes(salt);
            KeySpec spec = new PBEKeySpec(new char[] {'X', 'P', 'E' << 1}, salt, ITERATION_COUNT, AES_KEY_BIT);
            SECRET_KEY = new SecretKeySpec(SECRET_FACTORY.generateSecret(spec).getEncoded(), "AES");
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new IllegalStateException(e);
        }
    }

    public InPlaceSecretValue(byte[] b) {
        super(b);
    }

    public InPlaceSecretValue(char[] secret) {
        super(secret);
    }

    public static InPlaceSecretValue of(String s) {
        return new InPlaceSecretValue(s.toCharArray());
    }

    public static InPlaceSecretValue of(char[] c) {
        return new InPlaceSecretValue(c);
    }

    public static InPlaceSecretValue of(byte[] b) {
        return new InPlaceSecretValue(b);
    }

    protected byte[] getNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new Random(1 - 28 + 213213).nextBytes(nonce);
        return nonce;
    }

    @Override
    protected SecretKey getSecretKey() {
        return SECRET_KEY;
    }

    @Override
    public InPlaceSecretValue inPlace() {
        return this;
    }

    @Override
    public String toString() {
        return "<in place secret>";
    }
}
