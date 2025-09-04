package io.xpipe.app.util;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.process.CountDown;
import io.xpipe.app.process.ElevationHandler;
import io.xpipe.app.process.SecretReference;
import io.xpipe.app.secret.SecretManager;
import io.xpipe.app.secret.SecretQuery;
import io.xpipe.app.secret.SecretRetrievalStrategy;
import io.xpipe.app.storage.DataStorage;

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
    public boolean handleRequest(UUID requestId, CountDown countDown, boolean confirmIfNeeded, boolean interactive) {
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
                List.of(),
                countDown,
                interactive);
        return true;
    }

    @Override
    public SecretReference getSecretRef() {
        var id = DataStorage.get()
                .getStoreEntryIfPresent(dataStore, true)
                .or(() -> {
                    return DataStorage.get().getStoreEntryInProgressIfPresent(dataStore);
                })
                .map(e -> e.getUuid())
                .orElse(UUID.randomUUID());
        return password != null && password.expectsQuery() ? new SecretReference(id, 0) : null;
    }
}
