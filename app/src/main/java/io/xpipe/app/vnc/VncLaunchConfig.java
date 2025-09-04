package io.xpipe.app.vnc;

import io.xpipe.app.process.ShellControl;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.secret.SecretManager;
import io.xpipe.core.SecretValue;

import lombok.Value;

import java.util.Optional;

@Value
public class VncLaunchConfig {
    String title;
    String host;
    int port;
    DataStoreEntryRef<VncBaseStore> entry;
    ShellControl shellControl;

    public Optional<String> retrieveUsername() throws Exception {
        return Optional.ofNullable(entry.getStore().retrieveUser());
    }

    public boolean hasFixedPassword() {
        return entry.getStore().getPassword() != null
                && entry.getStore().getPassword().expectsQuery()
                && !entry.getStore().getPassword().query().requiresUserInteraction();
    }

    public boolean isTunneled() {
        return shellControl != null;
    }

    public Optional<SecretValue> retrievePassword() {
        var strat = entry.getStore().getPassword();
        if (!strat.expectsQuery()) {
            return Optional.empty();
        }

        var secret =
                SecretManager.retrieve(strat, "VNC login password", entry.get().getUuid(), 1, true);
        return Optional.ofNullable(secret);
    }
}
