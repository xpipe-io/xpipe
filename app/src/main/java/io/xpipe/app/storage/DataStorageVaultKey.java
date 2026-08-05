package io.xpipe.app.storage;

import io.xpipe.app.secret.DataStorageEncryptionKey;

import lombok.Value;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.SecretKey;

@Value
public class DataStorageVaultKey {

    public static DataStorageVaultKey generate() {
        var id = UUID.randomUUID().toString();
        var salt = new byte[32];
        new SecureRandom().nextBytes(salt);
        return new DataStorageVaultKey(id, salt, DataStorageEncryptionKey.getEncryptedKey(id.toCharArray(), salt));
    }

    public static DataStorageVaultKey load(Path file) throws IOException {
        var content = Files.readAllBytes(file);
        var parsed = new String(content, StandardCharsets.UTF_8);
        var lines = parsed.lines().toList();
        var id = lines.getFirst();
        var salt = Base64.getDecoder().decode(lines.get(1));
        var key = DataStorageEncryptionKey.getEncryptedKey(id.toCharArray(), salt);
        return new DataStorageVaultKey(id, salt, key);
    }

    public static void write(DataStorageVaultKey key, Path file) throws IOException {
        var s = key.getId();
        if (key.getSalt() != null) {
            s += "\n" + Base64.getEncoder().encodeToString(key.getSalt());
        }
        Files.writeString(file, s);
    }

    String id;
    byte[] salt;
    SecretKey key;
}
