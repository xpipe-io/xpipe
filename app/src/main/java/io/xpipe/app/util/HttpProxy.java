package io.xpipe.app.util;

import io.xpipe.app.ext.DataStore;
import io.xpipe.app.ext.ProcessControlProvider;
import io.xpipe.app.ext.ShellStore;
import io.xpipe.app.issue.ErrorEventFactory;
import io.xpipe.app.prefs.AppPrefs;
import io.xpipe.app.storage.DataStorage;
import io.xpipe.app.storage.DataStoreEntryRef;
import io.xpipe.core.SecretValue;
import lombok.Value;

import java.util.Optional;

@Value
public class HttpProxy {

    public static Optional<HttpProxy> getActiveProxy() {
        if (AppPrefs.get() == null) {
            return Optional.empty();
        }

        var current = AppPrefs.get().httpProxy().getValue();
        if (current == null) {
            return Optional.empty();
        }

        var found = DataStorage.get().getStoreEntryIfPresent(current);
        if (found.isEmpty()) {
            return Optional.empty();
        }

        try {
            var proxy = ProcessControlProvider.get().getHttpProxy(found.get().ref());
            return proxy;
        } catch (Exception e) {
            ErrorEventFactory.fromThrowable(e).handle();
            return Optional.empty();
        }
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

    String host;
    int port;
    String user;
    SecretValue password;
}
