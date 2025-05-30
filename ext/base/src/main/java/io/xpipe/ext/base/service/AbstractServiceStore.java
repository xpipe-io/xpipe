package io.xpipe.ext.base.service;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.HostHelper;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.Validators;
import io.xpipe.core.store.DataStore;
import io.xpipe.app.ext.NetworkTunnelSession;
import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.ext.SingletonSessionStore;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@EqualsAndHashCode
@ToString
public abstract class AbstractServiceStore implements SingletonSessionStore<NetworkTunnelSession>, DataStore {

    public abstract DataStoreEntryRef<NetworkTunnelStore> getHost();

    private final Integer remotePort;
    private final Integer localPort;
    private final ServiceProtocolType serviceProtocolType;

    public boolean licenseRequired() {
        return true;
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(getHost());
        Validators.isType(getHost(), NetworkTunnelStore.class);
        Validators.nonNull(remotePort);
        Validators.nonNull(serviceProtocolType);
    }

    public boolean requiresTunnel() {
        return getHost().getStore().requiresTunnel();
    }

    @Override
    public NetworkTunnelSession newSession() throws Exception {
        var f = LicenseProvider.get().getFeature("services");
        if (licenseRequired() && !f.isSupported()) {
            var active = DataStorage.get().getStoreEntries().stream()
                    .filter(dataStoreEntry -> dataStoreEntry.getStore() instanceof AbstractServiceStore a
                            && a != this
                            && a.licenseRequired()
                            && a.isSessionRunning())
                    .count();
            if (active > 0) {
                f.throwIfUnsupported();
            }
        }

        var l = localPort != null ? localPort : HostHelper.findRandomOpenPortOnAllLocalInterfaces();
        return getHost().getStore().createTunnelSession(l, remotePort, "localhost");
    }

    @Override
    public Class<?> getSessionClass() {
        return NetworkTunnelSession.class;
    }
}
