package io.xpipe.app.util;

import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.core.util.SecretReference;

import java.util.Optional;

public interface SecretQuery {

    static SecretQuery confirmElevationIfNeeded(SecretQuery original, boolean needed) {
        if (!needed) {
            return original;
        }

        return confirmElevation(original);
    }

    static SecretQuery confirmElevation(SecretQuery original) {
        return new SecretQuery() {

            @Override
            public Optional<SecretQueryResult> retrieveCache(String prompt, SecretReference reference) {
                var found = SecretQuery.super.retrieveCache(prompt, reference);
                if (found.isEmpty()) {
                    return Optional.empty();
                }

                var ask = AppPrefs.get().alwaysConfirmElevation().getValue();
                if (!ask) {
                    return found;
                }

                var inPlace = found.get().getSecret().inPlace();
                var r = AskpassAlert.queryRaw(prompt, inPlace);
                return r.isCancelled() ? Optional.of(r) : found;
            }

            @Override
            public SecretQueryResult query(String prompt) {
                var r = original.query(prompt);
                if (r.isCancelled()) {
                    return r;
                }

                var inPlace = r.getSecret().inPlace();
                return AskpassAlert.queryRaw(prompt, inPlace);
            }

            @Override
            public boolean cache() {
                return true;
            }

            @Override
            public boolean retryOnFail() {
                return true;
            }
        };
    }

    static SecretQuery prompt(boolean cache) {
        return new SecretQuery() {
            @Override
            public SecretQueryResult query(String prompt) {
                return AskpassAlert.queryRaw(prompt, null);
            }

            @Override
            public boolean cache() {
                return cache;
            }

            @Override
            public boolean retryOnFail() {
                return true;
            }
        };
    }

    default Optional<SecretQueryResult> retrieveCache(String prompt, SecretReference reference) {
        var r = SecretManager.get(reference);
        return r.map(secretValue -> new SecretQueryResult(secretValue, false));
    }

    SecretQueryResult query(String prompt);

    boolean cache();

    boolean retryOnFail();

    default boolean respectDontCacheSetting() {
        return true;
    }
}
