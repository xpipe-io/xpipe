package io.xpipe.core.util;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Random;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class AesSecretValue extends EncryptedSecretValue {

    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int AES_KEY_BIT = 128;
    private static final int SALT_BIT = 16;
    private static final SecretKeyFactory SECRET_FACTORY;

    static {
        try {
            SECRET_FACTORY = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    public AesSecretValue(char[] secret) {
        super(secret);
    }

    public AesSecretValue(byte[] b) {
        super(b);
    }

    protected abstract int getIterationCount();

    protected byte[] getNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    protected SecretKey getSecretKey(char[] chars) throws InvalidKeySpecException {
        var salt = new byte[SALT_BIT];
        new Random(AES_KEY_BIT).nextBytes(salt);
        KeySpec spec = new PBEKeySpec(chars, salt, getIterationCount(), AES_KEY_BIT);
        return new SecretKeySpec(SECRET_FACTORY.generateSecret(spec).getEncoded(), "AES");
    }

    protected SecretKey getAESKey() throws InvalidKeySpecException {
        throw new UnsupportedOperationException();
    }

    @Override
    @SneakyThrows
    public byte[] encrypt(byte[] c) {
        SecretKey secretKey = getAESKey();
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        var iv = getNonce(IV_LENGTH_BYTE);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        var bytes = cipher.doFinal(c);
        bytes = ByteBuffer.allocate(iv.length + bytes.length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(iv)
                .put(bytes)
                .array();
        return bytes;
    }

    @Override
    @SneakyThrows
    public byte[] decrypt(byte[] c) {
        ByteBuffer bb = ByteBuffer.wrap(c).order(ByteOrder.LITTLE_ENDIAN);
        byte[] iv = new byte[IV_LENGTH_BYTE];
        bb.get(iv);
        byte[] cipherText = new byte[bb.remaining()];
        bb.get(cipherText);

        SecretKey secretKey = getAESKey();
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        return cipher.doFinal(cipherText);
    }
}
