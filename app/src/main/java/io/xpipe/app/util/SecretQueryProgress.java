package io.xpipe.app.util;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.CountDown;
import io.xpipe.core.util.SecretReference;
import io.xpipe.core.util.SecretValue;
import lombok.Getter;

import java.util.*;

@Getter
public class SecretQueryProgress {

    private final UUID requestId;
    private final UUID storeId;
    private final List<SecretQuery> suppliers;
    private final SecretQuery fallback;
    private final List<String> seenPrompts;
    private final CountDown countDown;
    private boolean requestCancelled;

    public SecretQueryProgress(UUID requestId, UUID storeId, List<SecretQuery> suppliers, SecretQuery fallback, CountDown countDown) {
        this.requestId = requestId;
        this.storeId = storeId;
        this.suppliers = new ArrayList<>(suppliers);
        this.fallback = fallback;
        this.countDown = countDown;
        this.seenPrompts = new ArrayList<>();
    }

    public void advance(int count) {
        for (int i = 0; i < count; i++) {
            seenPrompts.add(null);
            suppliers.add(SecretQuery.prompt(false));
        }
    }

    public SecretValue process(String prompt) {
        // Cancel early
        if (requestCancelled) {
            return null;
        }

        var seenBefore = seenPrompts.contains(prompt);
        if (!seenBefore) {
            seenPrompts.add(prompt);
        }

        var firstSeenIndex = seenPrompts.indexOf(prompt);
        if (firstSeenIndex >= suppliers.size()) {
            countDown.pause();
            var r = fallback.query(prompt);
            countDown.resume();
            if (r.isCancelled()) {
                requestCancelled = true;
                return null;
            }
            return r.getSecret();
        }

        var ref = new SecretReference(storeId, firstSeenIndex);
        var sup = suppliers.get(firstSeenIndex);
        var shouldCache = sup.cache() && SecretManager.shouldCacheForPrompt(prompt) && !AppPrefs.get().dontCachePasswords().get();
        var wasLastPrompt = firstSeenIndex == seenPrompts.size() - 1;

        // Clear cache if secret was wrong/queried again
        // Check whether this is actually the last prompt seen as it might happen that
        // previous prompts get rolled back again when one further down is wrong
        if (seenBefore && shouldCache && wasLastPrompt) {
            SecretManager.clear(ref);
        }

        // If we supplied a wrong secret and cannot retry, cancel the entire request
        if (seenBefore && wasLastPrompt && !sup.retryOnFail()) {
            requestCancelled = true;
            return null;
        }

        if (shouldCache) {
            var cached = SecretManager.get(ref);
            if (cached.isPresent()) {
                return cached.get();
            }
        }

        countDown.pause();
        var r = sup.query(prompt);
        countDown.resume();

        if (r.isCancelled()) {
            requestCancelled = true;
            return null;
        }

        if (shouldCache) {
            SecretManager.set(ref, r.getSecret());
        }
        return r.getSecret();
    }
}
