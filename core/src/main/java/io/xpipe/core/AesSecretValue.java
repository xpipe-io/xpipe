package io.xpipe.core;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.SecureRandom;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;

@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public abstract class AesSecretValue extends EncryptedSecretValue {

    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;

    public AesSecretValue(String encryptedValue) {
        super(encryptedValue);
    }

    public AesSecretValue(char[] secret) {
        super(secret);
    }

    public AesSecretValue(byte[] b) {
        super(b);
    }

    protected byte[] getNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new SecureRandom().nextBytes(nonce);
        return nonce;
    }

    protected abstract SecretKey getSecretKey();

    @Override
    @SneakyThrows
    public byte[] encrypt(byte[] c) {
        SecretKey secretKey = getSecretKey();
        if (secretKey == null) {
            throw new IllegalStateException("Missing secret key");
        }

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

        SecretKey secretKey = getSecretKey();
        if (secretKey == null) {
            throw new IllegalStateException("Missing secret key");
        }

        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        return cipher.doFinal(cipherText);
    }
}
