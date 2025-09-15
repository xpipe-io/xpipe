package io.xpipe.app.ext;

import io.xpipe.app.core.AppI18n;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;

import java.util.Optional;

public interface NetworkTunnelStore extends DataStore, SelfReferentialStore {

    static void checkTunneable(DataStoreEntryRef<?> ref) throws ValidationException {
        if (!(ref.getStore() instanceof NetworkTunnelStore t)) {
            throw new ValidationException(AppI18n.get("parentHostDoesNotSupportTunneling", ref.get().getName()));
        }

        var unsupported = t.getUnsupportedParent();
        if (unsupported.isPresent()) {
            throw new ValidationException(AppI18n.get("parentHostDoesNotSupportTunneling", unsupported.get().get().getName()));
        }
    }

    DataStoreEntryRef<?> getNetworkParent();

    default boolean requiresTunnel() {
        return getNetworkParent() != null;
    }

    default String getTunnelHostName() {
        return null;
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

    NetworkTunnelSession createTunnelSession(int localPort, int remotePort, String address) throws Exception;
}
