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

    public static synchronized SecretQueryProgress expectAskpass(
            UUID request, UUID storeId, List<SecretQuery> suppliers, SecretQuery fallback, List<SecretQueryFilter> filters, CountDown countDown) {
        var p = new SecretQueryProgress(request, storeId, suppliers, fallback, filters, countDown);
        progress.add(p);
        return p;
    }

    public static boolean isSpecialPrompt(String prompt) {
        var l = prompt.toLowerCase(Locale.ROOT);
        // 2FA
        if (l.contains("passcode") || l.contains("verification code")) {
            return true;
        }

        // SSH host key trust prompt
        if (l.contains("authenticity of host") || l.contains("please type 'yes', 'no' or the fingerprint")) {
            return true;
        }

        return false;
    }

    public static SecretValue retrieve(SecretRetrievalStrategy strategy, String prompt, UUID secretId, int sub) {
        if (!strategy.expectsQuery()) {
            return null;
        }

        var uuid = UUID.randomUUID();
        var p = expectAskpass(uuid, secretId, List.of(strategy.query()), SecretQuery.prompt(false), List.of(), CountDown.of());
        p.preAdvance(sub);
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
