package io.xpipe.app.pwman;

import io.xpipe.app.cred.SshIdentityAgentStrategy;

import java.nio.file.Path;

public interface PasswordManagerKeyConfiguration {

    static PasswordManagerKeyConfiguration of(
            boolean inline,
            boolean joined,
            boolean supportsAgentKeyNames,
            PasswordManagerKeyStrategy strategy,
            Path socket) {
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
            public boolean supportsAgentKeyNames() {
                return supportsAgentKeyNames;
            }

            @Override
            public SshIdentityAgentStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
                return strategy.getSshIdentityStrategy(publicKey, forward);
            }

            @Override
            public Path getDefaultSocketLocation() {
                return socket;
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
            public boolean supportsAgentKeyNames() {
                return false;
            }

            @Override
            public SshIdentityAgentStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
                return null;
            }

            @Override
            public Path getDefaultSocketLocation() {
                return null;
            }
        };
    }

    boolean useInline();

    boolean useAgent();

    boolean supportsAgentKeyNames();

    SshIdentityAgentStrategy getSshIdentityStrategy(String publicKey, boolean forward);

    Path getDefaultSocketLocation();
}
