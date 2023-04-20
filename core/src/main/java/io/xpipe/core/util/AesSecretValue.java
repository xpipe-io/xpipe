package io.xpipe.core.util;

import lombok.EqualsAndHashCode;
import lombok.SneakyThrows;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Random;

@SuperBuilder
@Jacksonized
@EqualsAndHashCode(callSuper = true)
public class AesSecretValue extends EncryptedSecretValue {

    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int AES_KEY_BIT = 128;
    private static final byte[] IV = getFixedNonce(IV_LENGTH_BYTE);

    public AesSecretValue(char[] secret) {
        super(secret);
    }

    private static byte[] getFixedNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new Random(1 - 28 + 213213).nextBytes(nonce);
        return nonce;
    }

    protected SecretKey getAESKey(int keysize) throws NoSuchAlgorithmException, InvalidKeySpecException {
        throw new UnsupportedOperationException();
    }

    @Override
    @SneakyThrows
    public byte[] encrypt(byte[] c) {
        SecretKey secretKey = getAESKey(AES_KEY_BIT);
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, IV));
        var bytes = cipher.doFinal(c);
        bytes = ByteBuffer.allocate(IV.length + bytes.length)
                .order(ByteOrder.LITTLE_ENDIAN)
                .put(IV)
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

        SecretKey secretKey = getAESKey(AES_KEY_BIT);
        Cipher cipher = Cipher.getInstance(ENCRYPT_ALGO);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(TAG_LENGTH_BIT, iv));
        return cipher.doFinal(cipherText);
    }
}
