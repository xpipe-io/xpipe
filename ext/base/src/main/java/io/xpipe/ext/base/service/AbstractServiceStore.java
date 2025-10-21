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

import io.xpipe.ext.base.host.HostAddressStore;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.ToString;
import lombok.experimental.SuperBuilder;

@SuperBuilder(toBuilder = true)
@Getter
@EqualsAndHashCode
@ToString
public abstract class AbstractServiceStore implements SingletonSessionStore<NetworkTunnelSession>, DataStore {

    private final Integer remotePort;
    private final Integer localPort;
    private final ServiceProtocolType serviceProtocolType;

    public abstract String getAddress();

    public abstract DataStoreEntryRef<NetworkTunnelStore> getGateway();

    public abstract DataStoreEntryRef<?> getHost();

    public boolean licenseRequired() {
        return true;
    }

    @Override
    public void checkComplete() throws Throwable {
        Validators.nonNull(remotePort);
        Validators.nonNull(serviceProtocolType);
        if (getHost() != null) {
            getHost().checkComplete();
        } else {
            Validators.nonNull(getAddress());
        }
    }

    public String getOpenTargetUrl() {
        return ServiceAddressRotation.getRotatedAddress(this);
    }

    public boolean requiresTunnel() {
        if (getAddress() != null) {
            var gateway = getGateway();
            if (gateway != null) {
                return gateway.getStore().requiresTunnel();
            } else {
                return false;
            }
        }

        if (getHost() == null || !(getHost().getStore() instanceof NetworkTunnelStore t)) {
            return false;
        }

        if (!t.isLocallyTunnelable()) {
            var parent = t.getNetworkParent();
            if (!(parent instanceof NetworkTunnelStore nts)) {
                return false;
            }

            return nts.requiresTunnel();
        } else {
            return t.requiresTunnel();
        }
    }

    @Override
    public NetworkTunnelSession newSession() {
        if (getAddress() != null) {
            if (getGateway() == null || !getGateway().getStore().isLocallyTunnelable()) {
                return null;
            }
        }

        if (getHost() != null) {
            if (!(getHost().getStore() instanceof NetworkTunnelStore t)) {
                return null;
            }

            if (!t.isLocallyTunnelable()) {
                var parent = t.getNetworkParent();
                if (!(parent instanceof NetworkTunnelStore)) {
                    return null;
                }
            }
        }

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

        if (getAddress() != null) {
            var gateway = getGateway();
            return gateway.getStore().createTunnelSession(l, remotePort, getAddress());
        }

        var t = (NetworkTunnelStore) getHost().getStore();
        var parent = t.getNetworkParent();
        if (!t.isLocallyTunnelable() && parent instanceof NetworkTunnelStore nts) {
            return nts.createTunnelSession(
                    l, remotePort, t.getTunnelHostName() != null ? t.getTunnelHostName() : "localhost");
        }

        return t.createTunnelSession(l, remotePort, "localhost");
    }

    @Override
    public Class<?> getSessionClass() {
        return NetworkTunnelSession.class;
    }
}
