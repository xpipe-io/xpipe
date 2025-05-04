package io.xpipe.app.util;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.process.CountDown;
import io.xpipe.core.util.SecretReference;
import io.xpipe.core.util.SecretValue;

import lombok.Getter;
import lombok.NonNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
public class SecretQueryProgress {

    private final UUID requestId;
    private final UUID storeId;
    private final List<SecretQuery> suppliers;
    private final SecretQuery fallback;
    private final List<SecretQueryFilter> filters;
    private final List<SecretQueryFormatter> formatters;
    private final List<String> seenPrompts;
    private final CountDown countDown;
    private final boolean interactive;
    private SecretQueryState state = SecretQueryState.NORMAL;

    public SecretQueryProgress(
            @NonNull UUID requestId,
            @NonNull UUID storeId,
            @NonNull List<SecretQuery> suppliers,
            @NonNull SecretQuery fallback,
            @NonNull List<SecretQueryFilter> filters,
            List<SecretQueryFormatter> formatters,
            @NonNull CountDown countDown,
            boolean interactive) {
        this.requestId = requestId;
        this.storeId = storeId;
        this.suppliers = new ArrayList<>(suppliers);
        this.fallback = fallback;
        this.filters = filters;
        this.formatters = formatters;
        this.countDown = countDown;
        this.interactive = interactive;
        this.seenPrompts = new ArrayList<>();
    }

    public void preAdvance(int count) {
        for (int i = 0; i < count; i++) {
            seenPrompts.addFirst(null);
            suppliers.addFirst(SecretQuery.prompt(false));
        }
    }

    public SecretValue process(String prompt) {
        // Cancel early
        if (state != SecretQueryState.NORMAL) {
            return null;
        }

        for (SecretQueryFilter filter : filters) {
            var o = filter.filter(this, prompt);
            if (o.isPresent()) {
                return o.get();
            }
        }

        for (var formatter : formatters) {
            var r = formatter.format(prompt);
            if (r.isPresent()) {
                prompt = r.get();
            }
        }

        var seenBefore = seenPrompts.contains(prompt);
        if (!seenBefore) {
            seenPrompts.add(prompt);
        }

        var firstSeenIndex = seenPrompts.indexOf(prompt);
        if (firstSeenIndex >= suppliers.size()) {
            // Check whether we can have user inputs
            if (!interactive && fallback.requiresUserInteraction()) {
                state = SecretQueryState.NON_INTERACTIVE;
                return null;
            }

            countDown.pause();
            var r = fallback.query(prompt);
            countDown.resume();

            if (r.getState() != SecretQueryState.NORMAL) {
                state = r.getState();
                return null;
            }
            return r.getSecret();
        }

        var ref = new SecretReference(storeId, firstSeenIndex);
        var sup = suppliers.get(firstSeenIndex);
        var shouldCache = shouldCache(sup, prompt);
        var wasLastPrompt = firstSeenIndex == seenPrompts.size() - 1;

        // Check whether we can have user inputs
        if (!interactive && sup.requiresUserInteraction()) {
            state = SecretQueryState.NON_INTERACTIVE;
            return null;
        }

        // Clear cache if secret was wrong/queried again
        // Check whether this is actually the last prompt seen as it might happen that
        // previous prompts get rolled back again when one further down is wrong
        if (seenBefore && shouldCache && wasLastPrompt) {
            SecretManager.clear(ref);
        }

        // If we supplied a wrong secret and cannot retry, cancel the entire request
        if (seenBefore && wasLastPrompt && !sup.retryOnFail()) {
            state = SecretQueryState.FIXED_SECRET_WRONG;
            return null;
        }

        if (shouldCache) {
            countDown.pause();
            var cached = sup.retrieveCache(prompt, ref);
            countDown.resume();
            if (cached.isPresent()) {
                if (cached.get().getState() != SecretQueryState.NORMAL) {
                    state = cached.get().getState();
                    return null;
                }

                return cached.get().getSecret();
            }
        }

        countDown.pause();
        var r = sup.query(prompt);
        countDown.resume();

        if (r.getState() != SecretQueryState.NORMAL) {
            state = r.getState();
            return null;
        }

        if (shouldCache) {
            SecretManager.cache(ref, r.getSecret(), sup.cacheDuration());
        }
        return r.getSecret();
    }

    private boolean shouldCache(SecretQuery query, String prompt) {
        var hasDuration = query.cacheDuration() == null || query.cacheDuration().isPositive();
        var shouldCache = hasDuration
                && !SecretManager.disableCachingForPrompt(prompt)
                && (!query.respectDontCacheSetting()
                        || !AppPrefs.get().dontCachePasswords().get());
        return shouldCache;
    }
}
