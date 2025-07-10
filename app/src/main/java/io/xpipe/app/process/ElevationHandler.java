package io.xpipe.app.process;

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
}
