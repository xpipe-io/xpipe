package io.xpipe.app.store;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.app.util.HostAddress;
import io.xpipe.app.util.ValidationException;

import java.util.Optional;

public interface NetworkTunnelStore extends DataStore, SelfReferentialStore {

    static void checkTunnelable(DataStoreEntryRef<?> ref) throws ValidationException {
        if (!(ref.getStore() instanceof NetworkTunnelStore t)) {
            throw new ValidationException(
                    AppI18n.get("parentHostDoesNotSupportTunneling", ref.get().getName()));
        }

        var unsupported = t.getUnsupportedParent();
        if (unsupported.isPresent()) {
            throw new ValidationException(AppI18n.get(
                    "parentHostDoesNotSupportTunneling", unsupported.get().get().getName()));
        }
    }

    DataStoreEntryRef<?> getNetworkParent();

    boolean requiresTunnel();

    default HostAddress getTunnelHostName() {
        return HostAddress.empty();
    }

    default Optional<DataStoreEntryRef<NetworkTunnelStore>> getUnsupportedParent() {
        DataStoreEntryRef<NetworkTunnelStore> current = getSelfEntry().ref();
        while (true) {
            var p = current.getStore().getNetworkParent();
            if (p == null) {
                return Optional.empty();
            }

            if (p.getStore() instanceof NetworkTunnelStore) {
                current = p.asNeeded();
            } else {
                return Optional.of(current);
            }
        }
    }

    default boolean isLocallyTunnelable() {
        return getUnsupportedParent().isEmpty();
    }

    NetworkTunnelSession createTunnelSession(int localPort, int remotePort, String address);
}
