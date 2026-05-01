package io.xpipe.app.util;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.InPlaceSecretValue;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.extern.jackson.Jacksonized;

import java.util.Optional;

@Value
@Builder
@Jacksonized
@AllArgsConstructor
public class HttpProxy {

    public static Optional<HttpProxy> getActiveProxy() {
        if (AppPrefs.get() == null) {
            return Optional.empty();
        }

        var current = AppPrefs.get().httpProxy().getValue();
        if (current == null) {
            return Optional.empty();
        }

        return Optional.of(current);
    }

    public static boolean canUseAsProxy(DataStoreEntryRef<DataStore> ref) {
        if (!ref.get().getValidity().isUsable()) {
            return false;
        }

        try {
            return ProcessControlProvider.get().getHttpProxy(ref).isPresent();
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return false;
        }
    }

    public String toUrl() {
        return (socks5 ? "socks5" : "https") + "://"
                + (user != null && password != null ? user + ":" + password.getSecretValue() + "@" : "") + host + ":"
                + port;
    }

    String host;
    int port;
    String user;
    InPlaceSecretValue password;
    boolean socks5;
}
