package io.xpipe.core.process;

import io.xpipe.core.util.SecretReference;

import java.util.UUID;

public interface ElevationHandler {

    default ElevationHandler orElse(ElevationHandler other) {
        return new ElevationHandler() {

            @Override
            public boolean handleRequest(UUID requestId, CountDown countDown, boolean confirmIfNeeded) {
                var r = ElevationHandler.this.handleRequest(requestId, countDown, confirmIfNeeded);
                return r || other.handleRequest(requestId, countDown, confirmIfNeeded);
            }

            @Override
            public SecretReference getSecretRef() {
                var r = ElevationHandler.this.getSecretRef();
                return r != null ? r : other.getSecretRef();
            }
        };
    }

    boolean handleRequest(UUID requestId, CountDown countDown, boolean confirmIfNeeded);

    SecretReference getSecretRef();
}
