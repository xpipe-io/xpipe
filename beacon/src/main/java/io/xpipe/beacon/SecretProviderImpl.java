package io.xpipe.beacon;

import io.xpipe.core.util.SecretProvider;
import lombok.SneakyThrows;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Random;

public class SecretProviderImpl extends SecretProvider {

    private static final String ENCRYPT_ALGO = "AES/GCM/NoPadding";
    private static final int TAG_LENGTH_BIT = 128;
    private static final int IV_LENGTH_BYTE = 12;
    private static final int AES_KEY_BIT = 128;
    private static final byte[] IV = getFixedNonce(IV_LENGTH_BYTE);

    private static byte[] getFixedNonce(int numBytes) {
        byte[] nonce = new byte[numBytes];
        new SecureRandom(new byte[] {1, -28, 123}).nextBytes(nonce);
        return nonce;
    }

    private static SecretKey getAESKey(int keysize) throws NoSuchAlgorithmException, InvalidKeySpecException {
        SecretKeyFactory factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        var salt = new byte[16];
        new Random(keysize).nextBytes(salt);
        KeySpec spec = new PBEKeySpec(new char[] {'X', 'P', 'E' << 1}, salt, 65536, keysize);
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
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
