package io.xpipe.app.secret;

import lombok.SneakyThrows;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Random;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public class EncryptionKey {

    @SneakyThrows
    public static SecretKey getLegacyEncryptedKey(char[] password) {
        // This is no longer used anywhere, only for very old vaults that were not migrated yet
        int iterations = 8192;
        var salt = new byte[16];
        new Random(128).nextBytes(salt);
        var spec = new PBEKeySpec(password, salt, iterations, 128);
        var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    @SneakyThrows
    public static SecretKey getLegacyVaultSecretKey(String vaultId) {
        // This is no longer used anywhere, only for very old vaults that were not migrated yet
        int iterations = 8192;
        var salt = new byte[16];
        new Random(128).nextBytes(salt);
        var spec = new PBEKeySpec(vaultId.toCharArray(), salt, iterations, 128);
        var factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256");
        return new SecretKeySpec(factory.generateSecret(spec).getEncoded(), "AES");
    }

    @SneakyThrows
    public static SecretKey getLegacyEncryptedKey(char[] password, byte[] salt) {
        String algorithm = "PBKDF2WithHmacSHA256";
        int derivedKeyLength = 256;
        // https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#pbkdf2
        int iterations = 600000;
        var spec = new PBEKeySpec(password, salt, iterations, derivedKeyLength);
        var f = SecretKeyFactory.getInstance(algorithm);
        return new SecretKeySpec(f.generateSecret(spec).getEncoded(), "AES");
    }

    @SneakyThrows
    public static SecretKey getEncryptedKey(char[] password, byte[] salt) {
        // https://cheatsheetseries.owasp.org/cheatsheets/Password_Storage_Cheat_Sheet.html#argon2id
        Argon2Parameters.Builder builder = new Argon2Parameters.Builder(Argon2Parameters.ARGON2_id)
                .withVersion(Argon2Parameters.ARGON2_VERSION_13)
                .withMemoryAsKB(12288)
                .withParallelism(1)
                .withIterations(3)
                .withSalt(salt);
        Argon2Parameters params = builder.build();

        Argon2BytesGenerator gen = new Argon2BytesGenerator();
        gen.init(params);
        byte[] result = new byte[32];
        gen.generateBytes(password, result, 0, result.length);
        return new SecretKeySpec(result, "AES");
    }
}
