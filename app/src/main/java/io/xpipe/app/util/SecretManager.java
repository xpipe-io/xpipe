package io.xpipe.app.util;

import io.xpipe.core.util.SecretValue;
import io.xpipe.core.util.UuidHelper;
import lombok.AllArgsConstructor;
import lombok.Value;

import java.util.*;

public class SecretManager {

    @Value
    @AllArgsConstructor
    public static class SecretReference {

        UUID secretId;
        int subId;

        public SecretReference(Object store) {
            this.secretId = UuidHelper.generateFromObject(store);
            this.subId = 0;
        }

        public SecretReference(Object store, int sub) {
            this.secretId = UuidHelper.generateFromObject(store);
            this.subId = sub;
        }
    }

    private static final Map<SecretReference, SecretValue> passwords = new HashMap<>();

    public static boolean shouldCacheForPrompt(String prompt) {
        var l = prompt.toLowerCase(Locale.ROOT);
        if (l.contains("passcode") || l.contains("verification code")) {
            return false;
        }

        return true;
    }

    public static SecretValue retrieve(SecretRetrievalStrategy strategy, String prompt, Object key) throws Exception {
        return retrieve(strategy, prompt,key, 0);
    }
    public static SecretValue retrieve(SecretRetrievalStrategy strategy, String prompt, Object key, int sub) throws Exception {
        var ref = new SecretReference(key, sub);
        if (strategy == null) {
            return null;
        }

        if (strategy.shouldCache() && passwords.containsKey(ref)) {
            return passwords.get(ref);
        }

        var pass = strategy.retrieve(prompt, ref.getSecretId(), ref.getSubId());
        if (pass == null) {
            return null;
        }

        if (strategy.shouldCache()) {
            passwords.put(ref, pass);
        }
        return pass;
    }

    public static void clearAll(Object store) {
        var id = UuidHelper.generateFromObject(store);
        passwords.entrySet().removeIf(secretReferenceSecretValueEntry -> secretReferenceSecretValueEntry.getKey().getSecretId().equals(id));
    }

    public static void clear(SecretReference ref) {
        passwords.remove(ref);
    }

    public static void set(SecretReference ref, SecretValue value) {
        passwords.put(ref, value);
    }

    public static Optional<SecretValue> get(SecretReference ref) {
        return Optional.ofNullable(passwords.get(ref));
    }
}
