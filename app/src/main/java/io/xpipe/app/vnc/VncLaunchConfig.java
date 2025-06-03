package io.xpipe.app.vnc;

import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.process.ShellControl;
import io.xpipe.core.util.SecretValue;
import lombok.Value;

import java.util.Optional;

@Value
public class VncLaunchConfig {
    String title;
    String host;
    int port;
    DataStoreEntryRef<VncBaseStore> entry;
    ShellControl shellControl;

    public Optional<String> retrieveUsername() {
        return Optional.ofNullable(entry.getStore().getUser());
    }

    public boolean hasFixedPassword() {
        return entry.getStore().getPassword() != null &&
                entry.getStore().getPassword().expectsQuery() &&
                !entry.getStore().getPassword().query().requiresUserInteraction();
    }

    public boolean isTunneled() {
        return shellControl != null;
    }

    public Optional<SecretValue> retrievePassword() {
        var strat = entry.getStore().getPassword();
        if (!strat.expectsQuery()) {
            return Optional.empty();
        }

        var r = strat.query().query("Password for " + title);
        return Optional.ofNullable(r.getSecret());
    }
}
