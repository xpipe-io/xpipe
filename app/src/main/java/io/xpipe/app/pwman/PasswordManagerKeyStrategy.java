package io.xpipe.app.pwman;

import io.xpipe.app.cred.SshIdentityStrategy;

public interface PasswordManagerKeyStrategy {

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
