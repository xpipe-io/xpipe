package io.xpipe.app.pwman;

import io.xpipe.app.cred.SshIdentityStrategy;

public interface PasswordManagerKeyConfiguration {

    static PasswordManagerKeyConfiguration of(boolean inline, boolean joined, PasswordManagerKeyStrategy strategy) {
        return new PasswordManagerKeyConfiguration() {
            @Override
            public boolean useInline() {
                return (strategy == null || !strategy.useAgent()) && inline && joined;
            }

            @Override
            public boolean useAgent() {
                return strategy != null && strategy.useAgent();
            }

            @Override
            public SshIdentityStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
                return strategy.getSshIdentityStrategy(publicKey, forward);
            }
        };
    }

    static PasswordManagerKeyConfiguration none() {
        return new PasswordManagerKeyConfiguration() {
            @Override
            public boolean useInline() {
                return false;
            }

            @Override
            public boolean useAgent() {
                return false;
            }

            @Override
            public SshIdentityStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
                return null;
            }
        };
    }

    boolean useInline();

    boolean useAgent();

    SshIdentityStrategy getSshIdentityStrategy(String publicKey, boolean forward);
}
