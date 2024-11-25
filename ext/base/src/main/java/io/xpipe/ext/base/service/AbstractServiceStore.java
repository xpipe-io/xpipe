package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.HostHelper;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.Validators;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.NetworkTunnelSession;
import io.xpipe.core.store.NetworkTunnelStore;
import io.xpipe.core.store.SingletonSessionStore;
import io.xpipe.core.util.JacksonizedValue;

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
public abstract class AbstractServiceStore extends JacksonizedValue
        implements SingletonSessionStore<NetworkTunnelSession>, DataStore {

    public abstract DataStoreEntryRef<NetworkTunnelStore> getHost();

    private final Integer remotePort;
    private final Integer localPort;

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(getHost());
        Validators.isType(getHost(), NetworkTunnelStore.class);
        Validators.nonNull(remotePort);
    }

    public boolean requiresTunnel() {
        return getHost().getStore().requiresTunnel();
    }

    @Override
    public NetworkTunnelSession newSession() throws Exception {
        var f = LicenseProvider.get().getFeature("services");
        if (!f.isSupported()) {
            var active = DataStorage.get().getStoreEntries().stream()
                    .filter(dataStoreEntry -> dataStoreEntry.getStore() instanceof AbstractServiceStore a
                            && a != this
                            && a.isSessionRunning())
                    .count();
            if (active > 0) {
                f.throwIfUnsupported();
            }
        }

        var l = localPort != null ? localPort : HostHelper.findRandomOpenPortOnAllLocalInterfaces();
        return getHost().getStore().sessionChain(l, remotePort, "localhost");
    }

    @Override
    public Class<?> getSessionClass() {
        return NetworkTunnelSession.class;
    }
}
