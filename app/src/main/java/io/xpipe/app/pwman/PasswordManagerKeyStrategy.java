package io.xpipe.app.pwman;

import io.xpipe.app.cred.SshIdentityStrategy;

public interface PasswordManagerKeyStrategy {

    static PasswordManagerKeyStrategy of(boolean inline, boolean joined, PasswordManagerAgentStrategy strategy) {
        return new PasswordManagerKeyStrategy() {
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
            public SshIdentityStrategy getSshIdentityStrategy() {
                return strategy.getSshIdentityStrategy();
            }
        };
    }

    static PasswordManagerKeyStrategy none() {
        return new PasswordManagerKeyStrategy() {
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
            public SshIdentityStrategy getSshIdentityStrategy() {
                return null;
            }
        };
    }

    boolean supportsInlineSshKeys();

    boolean supportsAgent();

    boolean supportsJoinedEntries();

    SshIdentityStrategy getSshIdentityStrategy();
}
