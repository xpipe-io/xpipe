package io.xpipe.ext.base.service;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.hub.entry.StoreEntryBadge;
import io.xpipe.app.hub.entry.StoreEntryInformation;
import io.xpipe.app.hub.section.StoreSection;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntry;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.store.*;
import io.xpipe.app.util.DocumentationLink;
import io.xpipe.app.util.FailableRunnable;
import io.xpipe.ext.base.host.HostAddressGatewayStore;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractServiceStoreProvider implements SingletonSessionStoreProvider, DataStoreProvider {

    @Override
    public boolean showIncompleteInfo() {
        return true;
    }

    @Override
    public DocumentationLink getHelpLink() {
        return DocumentationLink.SERVICES;
    }

    @Override
    public boolean supportsSession(SingletonSessionStore<?> s) {
        var abs = (AbstractServiceStore) s;
        if (abs.getAddress() != null) {
            return abs.getGateway() != null
                    && abs.getGateway().getStore().isLocallyTunnelable()
                    && abs.getGateway().getStore().requiresTunnel();
        }

        if (abs.getHost() != null) {
            if (abs.getHost().asNeeded().getStore() instanceof LocalStore) {
                return false;
            }

            if (!abs.getHost().getStore().isComplete()) {
                return false;
            }

            if (abs.getHost().getStore() instanceof HostAddressGatewayStore a) {
                if (a.getTunnelGateway() != null
                        && a.getTunnelGateway().getStore().requiresTunnel()
                        && a.getTunnelGateway().getStore().isLocallyTunnelable()
                        && abs.shouldTunnel()) {
                    return true;
                }
            }

            if (abs.getHost().getStore() instanceof NetworkTunnelStore t) {
                if (!t.requiresTunnel()) {
                    return false;
                }

                if (!abs.shouldTunnel()) {
                    return false;
                }

                if (t.isLocallyTunnelable()) {
                    return true;
                }

                var parent = t.getNetworkParent();
                if (!t.isLocallyTunnelable() && parent.getStore() instanceof NetworkTunnelStore nts) {
                    return nts.isLocallyTunnelable();
                }

                return false;
            }
        }

        return false;
    }

    @Override
    public FailableRunnable<Exception> launch(DataStoreEntry store) {
        return () -> {
            AbstractServiceStore serviceStore = store.getStore().asNeeded();
            serviceStore.startSessionIfNeeded();
            var full = serviceStore.getServiceProtocolType().formatAddress(serviceStore.getOpenTargetUrl());
            serviceStore.getServiceProtocolType().open(full);
        };
    }

    public String displayName(DataStoreEntry entry) {
        AbstractServiceStore s = entry.getStore().asNeeded();
        return DataStorage.get().getStoreEntryDisplayName(s.getHost().get()) + " - Port " + s.getRemotePort();
    }

    @Override
    public List<String> getSearchableTerms(DataStore store) {
        AbstractServiceStore s = store.asNeeded();
        var l = new ArrayList<String>();
        l.add("" + s.getRemotePort());
        if (s.getLocalPort() != null) {
            l.add("" + s.getLocalPort());
        }
        if (s.getAddress() != null) {
            l.add(s.getAddress());
        }
        return l;
    }

    @Override
    public DataStoreUsageCategory getUsageCategory() {
        return DataStoreUsageCategory.TUNNEL;
    }

    @Override
    public DataStoreEntryRef<?> getSyntheticParent(DataStoreEntry store) {
        AbstractServiceStore s = store.getStore().asNeeded();
        return DataStorage.get()
                .getOrCreateNewSyntheticEntry(
                        s.getHost().get(),
                        "Services",
                        CustomServiceGroupStore.builder().parent(s.getHost()).build())
                .ref();
    }

    @Override
    public StoreEntryInformation buildInformation(StoreSection section) {
        AbstractServiceStore s = section.getEntry().getStore().asNeeded();
        var addr = formatAddress(s);
        var port = formatPortMapping(s);
        var type = s.getServiceProtocolType() != null
                        && !(s.getServiceProtocolType() instanceof ServiceProtocolType.Undefined)
                ? AppI18n.get(s.getServiceProtocolType().getTranslationKey())
                : null;
        var state = !s.requiresTunnel()
                ? null
                : s.isSessionRunning()
                        ? AppI18n.get("running")
                        : s.isSessionEnabled() ? AppI18n.get("starting") : AppI18n.get("inactive");
        return StoreEntryInformation.of(
                s.isSessionRunning() ? StoreEntryBadge.ofSuccess(state) : StoreEntryBadge.ofIndeterminant(state),
                StoreEntryBadge.ofStaticAddress(addr),
                StoreEntryBadge.ofSetting(type),
                StoreEntryBadge.ofSetting(port));
    }

    @Override
    public String getDisplayIconFileName(DataStore store) {
        return "base:service_icon.svg";
    }

    private String formatAddress(AbstractServiceStore s) {
        var desc = s.getLocalPort() != null
                ? "localhost:" + s.getLocalPort()
                : s.isSessionRunning() ? "localhost:" + s.getSession().getLocalPort() : null;
        return desc;
    }

    protected String formatPortMapping(AbstractServiceStore s) {
        return AppI18n.get("servicePort", s.getRemotePort());
    }
}
