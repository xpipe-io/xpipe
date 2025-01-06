package io.xpipe.app.util;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.process.CountDown;
import io.xpipe.core.process.ElevationHandler;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.SecretReference;

import java.util.List;
import java.util.UUID;

public class BaseElevationHandler implements ElevationHandler {

    private final DataStore dataStore;
    private final SecretRetrievalStrategy password;

    public BaseElevationHandler(DataStore dataStore, SecretRetrievalStrategy password) {
        this.dataStore = dataStore;
        this.password = password;
    }

    @Override
    public boolean handleRequest(ShellControl parent, UUID requestId, CountDown countDown, boolean confirmIfNeeded) {
        var ref = getSecretRef();
        if (ref == null) {
            return false;
        }

        SecretManager.expectAskpass(
                requestId,
                ref.getSecretId(),
                List.of(SecretQuery.confirmElevationIfNeeded(password.query(), confirmIfNeeded)),
                SecretQuery.prompt(false),
                List.of(),
                countDown,
                parent.isInteractive());
        return true;
    }

    @Override
    public SecretReference getSecretRef() {
        var id = DataStorage.get().getStoreEntryIfPresent(dataStore, true)
                .or(() -> {
                    return DataStorage.get().getStoreEntryInProgressIfPresent(dataStore);
                }).map(e -> e.getUuid()).orElse(UUID.randomUUID());
        return password != null && password.expectsQuery() ? new SecretReference(id, 0) : null;
    }
}
