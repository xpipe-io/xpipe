package io.xpipe.app.util;

import io.xpipe.core.util.SecretValue;
import io.xpipe.core.util.UuidHelper;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class SecretCache {

    private static final Map<UUID, SecretValue> passwords = new HashMap<>();

    public static SecretValue retrieve(SecretRetrievalStrategy strategy, String prompt, Object key) throws Exception {
        var id = UuidHelper.generateFromObject(key);
        if (passwords.containsKey(id)) {
            return passwords.get(id);
        }

        if (strategy == null) {
            return null;
        }

        var pass = strategy.retrieve(prompt, id);
        passwords.put(id, pass);
        return pass;
    }

    public static void clear(UUID id) {
        passwords.remove(id);
    }

    public static void set(UUID id, SecretValue value) {
        passwords.put(id, value);
    }

    public static Optional<SecretValue> get(UUID id) {
        return Optional.ofNullable(passwords.get(id));
    }
}
