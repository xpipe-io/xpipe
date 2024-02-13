package io.xpipe.app.util;

import io.xpipe.core.process.CountDown;
import io.xpipe.core.util.SecretReference;
import io.xpipe.core.util.SecretValue;
import io.xpipe.core.util.UuidHelper;

import java.util.*;

public class SecretManager {

    private static final Map<SecretReference, SecretValue> secrets = new HashMap<>();
    private static final Set<SecretQueryProgress> progress = new HashSet<>();

    public static Optional<SecretQueryProgress> getProgress(UUID requestId, UUID storeId) {
        return progress.stream()
                .filter(secretQueryProgress -> secretQueryProgress.getRequestId().equals(requestId) &&
                        secretQueryProgress.getStoreId().equals(storeId))
                .findFirst();
    }

    public static Optional<SecretQueryProgress> getProgress(UUID requestId) {
        return progress.stream()
                .filter(secretQueryProgress -> secretQueryProgress.getRequestId().equals(requestId))
                .findFirst();
    }

    public static SecretQueryProgress expectCacheablePrompt(UUID request, UUID storeId, CountDown countDown) {
        var p = new SecretQueryProgress(request, storeId, List.of(SecretQuery.prompt(true)), SecretQuery.prompt(false), countDown);
        progress.add(p);
        return p;
    }

    public static SecretQueryProgress expectAskpass(UUID request, UUID storeId, List<SecretQuery> suppliers, SecretQuery fallback, CountDown countDown) {
        var p = new SecretQueryProgress(request, storeId, suppliers, fallback, countDown);
        progress.add(p);
        return p;
    }

    public static boolean shouldCacheForPrompt(String prompt) {
        var l = prompt.toLowerCase(Locale.ROOT);
        if (l.contains("passcode") || l.contains("verification code")) {
            return false;
        }

        return true;
    }

    public static SecretValue retrieve(SecretRetrievalStrategy strategy, String prompt, Object store, int sub) {
        if (!strategy.expectsPrompt()) {
            return null;
        }

        var p = expectAskpass(UUID.randomUUID(), UuidHelper.generateFromObject(store, sub), List.of(strategy.query()), SecretQuery.prompt(false), CountDown.of());
        return p.process(prompt);
    }

    public static void clearAll(Object store) {
        var id = UuidHelper.generateFromObject(store);
        secrets.entrySet().removeIf(secretReferenceSecretValueEntry -> secretReferenceSecretValueEntry.getKey().getSecretId().equals(id));
    }

    public static void clear(SecretReference ref) {
        secrets.remove(ref);
    }

    public static void set(SecretReference ref, SecretValue value) {
        secrets.put(ref, value);
    }

    public static Optional<SecretValue> get(SecretReference ref) {
        return Optional.ofNullable(secrets.get(ref));
    }
}
