package io.xpipe.app.pwman;

import io.xpipe.app.cred.SshIdentityStrategy;

public interface PasswordManagerKeyConfiguration {

    static PasswordManagerKeyConfiguration of(boolean inline, boolean joined, PasswordManagerKeyStrategy strategy) {
        return new PasswordManagerKeyConfiguration() {
            @Override
            public boolean supportsInlineSshKeys() {
                return inline;
            }

            @Override
            public boolean supportsAgent() {
                return strategy != null;
            }

            @Override
            public boolean supportsJoinedEntries() {
                return joined;
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
            public boolean supportsInlineSshKeys() {
                return false;
            }

            @Override
            public boolean supportsAgent() {
                return false;
            }

            @Override
            public boolean supportsJoinedEntries() {
                return false;
            }

            @Override
            public SshIdentityStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
                return null;
            }
        };
    }

    boolean supportsInlineSshKeys();

    boolean supportsAgent();

    boolean supportsJoinedEntries();

    SshIdentityStrategy getSshIdentityStrategy(String publicKey, boolean forward);
}
