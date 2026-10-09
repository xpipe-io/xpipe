package io.xpipe.app.process;

import io.xpipe.app.secret.SecretManager;
import io.xpipe.app.util.SecretValue;

import java.util.Optional;
import java.util.UUID;

public interface ElevationHandler {

    default ElevationHandler orElse(ElevationHandler other) {
        return new ElevationHandler() {

            @Override
            public boolean handleRequest(
                    UUID requestId, CountDown countDown, boolean confirmIfNeeded, boolean interactive) {
                var r = ElevationHandler.this.handleRequest(requestId, countDown, confirmIfNeeded, interactive);
                return r || other.handleRequest(requestId, countDown, confirmIfNeeded, interactive);
            }

            @Override
            public SecretReference getSecretRef() {
                var r = ElevationHandler.this.getSecretRef();
                return r != null ? r : other.getSecretRef();
            }
        };
    }

    boolean handleRequest(UUID requestId, CountDown countDown, boolean confirmIfNeeded, boolean interactive);

    SecretReference getSecretRef();

    default Optional<SecretValue> retrieveSecret(UUID requestId, CountDown countDown, boolean confirmIfNeeded, boolean interactive, String displayUser) throws Exception {
        try {
            if (!handleRequest(requestId, countDown, confirmIfNeeded, interactive)) {
                return Optional.empty();
            }

            var progress = SecretManager.getProgress(requestId);
            if (progress.isEmpty()) {
                return Optional.empty();
            }

            return Optional.ofNullable(progress.get().process("[sudo] password for " + displayUser));
        } finally {
            SecretManager.completeRequest(requestId);
        }
    }
}
