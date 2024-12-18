package io.xpipe.app.util;

import lombok.SneakyThrows;

import java.security.spec.KeySpec;
import java.util.Random;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionKey {

    @SneakyThrows
    public static SecretKey getEncryptedKey(char[] password, byte[] salt) {
        String algorithm = "PBKDF2WithHmacSHA256";
        int derivedKeyLength = 256;
        // https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2
        int iterations = 600000;

        KeySpec spec = new PBEKeySpec(password, salt, iterations, derivedKeyLength);
        SecretKeyFactory f = SecretKeyFactory.getInstance(algorithm);
        return new SecretKeySpec(f.generateSecret(spec).getEncoded(), "AES");
    }

    @SneakyThrows
    public static SecretKey getLegacyEncryptedKey(char[] password) {
        int iterations = 8192;
        var salt = new byte[16];
        new Random(128).nextBytes(salt);
        KeySpec spec = new PBEKeySpec(password, salt, iterations, 128);
        var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    @SneakyThrows
    public static SecretKey getVaultSecretKey(String vaultId) {
        int iterations = 8192;
        var salt = new byte[16];
        new Random(128).nextBytes(salt);
        KeySpec spec = new PBEKeySpec(vaultId.toCharArray(), salt, iterations, 128);
        var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }
}
