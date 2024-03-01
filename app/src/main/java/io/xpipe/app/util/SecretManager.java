package io.xpipe.app.util;

import io.xpipe.app.issue.TrackEvent;
import io.xpipe.core.process.CountDown;
import io.xpipe.core.util.SecretReference;
import io.xpipe.core.util.SecretValue;
import io.xpipe.core.util.UuidHelper;

import java.util.*;

public class SecretManager {

    private static final Map<SecretReference, SecretValue> secrets = new HashMap<>();
    private static final Set<SecretQueryProgress> progress = new HashSet<>();

    public static synchronized Optional<SecretQueryProgress> getProgress(UUID requestId, UUID storeId) {
        return progress.stream()
                .filter(secretQueryProgress ->
                        secretQueryProgress.getRequestId().equals(requestId)
                                && secretQueryProgress.getStoreId().equals(storeId))
                .findFirst();
    }

    public static synchronized Optional<SecretQueryProgress> getProgress(UUID requestId) {
        return progress.stream()
                .filter(secretQueryProgress ->
                        secretQueryProgress.getRequestId().equals(requestId))
                .findFirst();
    }

    public static synchronized SecretQueryProgress expectElevationPrompt(
            UUID request, UUID secretId, CountDown countDown, boolean askIfNeeded) {
        var p = new SecretQueryProgress(
                request,
                secretId,
                List.of(askIfNeeded ? SecretQuery.elevation(secretId) : SecretQuery.prompt(true)),
                SecretQuery.prompt(false),
                countDown);
        progress.add(p);
        return p;
    }

    public static synchronized SecretQueryProgress expectAskpass(
            UUID request, UUID storeId, List<SecretQuery> suppliers, SecretQuery fallback, CountDown countDown) {
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

    public static SecretValue retrieve(SecretRetrievalStrategy strategy, String prompt, UUID secretId, int sub) {
        if (!strategy.expectsQuery()) {
            return null;
        }

        var uuid = UUID.randomUUID();
        var p = expectAskpass(uuid, secretId, List.of(strategy.query()), SecretQuery.prompt(false), CountDown.of());
        p.advance(sub);
        var r = p.process(prompt);
        completeRequest(uuid);
        return r;
    }

    public static synchronized void completeRequest(UUID request) {
        if (progress.removeIf(
                secretQueryProgress -> secretQueryProgress.getRequestId().equals(request))) {
            TrackEvent.withTrace("Completed secret request")
                    .tag("uuid", request)
                    .handle();
        }
    }

    public static synchronized void clearAll(Object store) {
        var id = UuidHelper.generateFromObject(store);
        secrets.entrySet()
                .removeIf(secretReferenceSecretValueEntry ->
                        secretReferenceSecretValueEntry.getKey().getSecretId().equals(id));
    }

    public static synchronized void clear(SecretReference ref) {
        secrets.remove(ref);
    }

    public static synchronized void set(SecretReference ref, SecretValue value) {
        secrets.put(ref, value);
    }

    public static synchronized Optional<SecretValue> get(SecretReference ref) {
        return Optional.ofNullable(secrets.get(ref));
    }
}
