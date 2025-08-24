package io.xpipe.ext.base.service;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.NetworkTunnelSession;
import io.xpipe.app.ext.NetworkTunnelStore;
import io.xpipe.app.ext.SingletonSessionStore;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.HostHelper;
import io.xpipe.app.util.LicenseProvider;
import io.xpipe.app.util.Validators;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Getter
@EqualsAndHashCode
@ToString
public abstract class AbstractServiceStore implements SingletonSessionStore<NetworkTunnelSession>, DataStore {

    private final Integer remotePort;
    private final Integer localPort;
    private final ServiceProtocolType serviceProtocolType;

    public abstract DataStoreEntryRef<NetworkTunnelStore> getHost();

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

    public String getOpenTargetUrl() {
        var s = getSession();
        if (s == null) {
            var host = getHost().getStore().getTunnelHostName() != null
                    ? getHost().getStore().getTunnelHostName()
                    : "localhost";
            return host + ":" + remotePort;
        }

        return "localhost:" + s.getLocalPort();
    }

    public boolean requiresTunnel() {
        if (getHost() == null) {
            return false;
        }

        if (!getHost().getStore().isLocallyTunnelable()) {
            var parent = getHost().getStore().getNetworkParent();
            if (!(parent instanceof NetworkTunnelStore nts)) {
                return false;
            }

            return nts.requiresTunnel();
        }

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

        var parent = getHost().getStore().getNetworkParent();
        if (!getHost().getStore().isLocallyTunnelable() && parent instanceof NetworkTunnelStore nts) {
            return nts.createTunnelSession(
                    l, remotePort, nts.getTunnelHostName() != null ? nts.getTunnelHostName() : "localhost");
        }

        return getHost().getStore().createTunnelSession(l, remotePort, "localhost");
    }

    @Override
    public Class<?> getSessionClass() {
        return NetworkTunnelSession.class;
    }
}
