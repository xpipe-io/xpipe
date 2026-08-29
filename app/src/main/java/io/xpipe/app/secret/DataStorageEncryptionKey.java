package io.xpipe.app.secret;

import lombok.SneakyThrows;
import org.bouncycastle.crypto.generators.Argon2BytesGenerator;
import org.bouncycastle.crypto.params.Argon2Parameters;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

public class DataStorageEncryptionKey {

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
