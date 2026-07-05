package io.xpipe.app.pwman;

import io.xpipe.app.cred.SshIdentityKeyListStrategy;

import java.nio.file.Path;

public interface PasswordManagerKeyConfiguration {

    static PasswordManagerKeyConfiguration of(
            boolean supportsInline,
            boolean supportedJoined,
            boolean supportsAgentKeyNames,
            PasswordManagerKeyStrategy strategy,
            Path socket) {
        return new PasswordManagerKeyConfiguration() {
            @Override
            public boolean supportsJoinedInformation() {
                return supportedJoined;
            }

            @Override
            public boolean useInline() {
                return (strategy == null || !strategy.useAgent()) && supportsInline && supportedJoined;
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
            public SshIdentityKeyListStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
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
            public boolean supportsJoinedInformation() {
                return false;
            }

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
            public SshIdentityKeyListStrategy getSshIdentityStrategy(String publicKey, boolean forward) {
                return null;
            }

            @Override
            public Path getDefaultSocketLocation() {
                return null;
            }
        };
    }

    boolean supportsJoinedInformation();

    boolean useInline();

    boolean useAgent();

    boolean supportsAgentKeyNames();

    SshIdentityKeyListStrategy getSshIdentityStrategy(String publicKey, boolean forward);

    Path getDefaultSocketLocation();
}
