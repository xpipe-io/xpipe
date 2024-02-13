package io.xpipe.app.util;

public interface SecretQuery {

    static SecretQuery prompt(boolean cache) {
        return new SecretQuery() {
            @Override
            public SecretQueryResult query(String prompt) {
                return AskpassAlert.queryRaw(prompt);
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

    SecretQueryResult query(String prompt);

    boolean cache();

    boolean retryOnFail();
}
