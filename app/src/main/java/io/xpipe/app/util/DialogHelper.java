package io.xpipe.app.util;

import io.xpipe.app.storage.DataStorage;
import io.xpipe.core.charsetter.NewLine;
import io.xpipe.core.charsetter.StreamCharset;
import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.store.DataFlow;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.FileSystem;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.SecretValue;
import lombok.Value;

public class DialogHelper {

    public static Dialog addressQuery(Address address) {
        var hostNameQuery = Dialog.query("Hostname", false, true, false, address.getHostname(), QueryConverter.STRING);
        var portQuery = Dialog.query("Port", false, true, false, address.getPort(), QueryConverter.INTEGER);
        return Dialog.chain(hostNameQuery, portQuery)
                .evaluateTo(() -> new Address(hostNameQuery.getResult(), portQuery.getResult()));
    }

    public static Dialog machineQuery(DataStore store) {
        var storeName = DataStorage.get().getStoreDisplayName(store).orElse("localhost");
        return Dialog.query("Machine", false, true, false, storeName, QueryConverter.STRING)
                .map((String name) -> {
                    if (name.equals("local") || name.equals("localhost")) {
                        return new LocalStore();
                    }

                    var stored = DataStorage.get().getStoreEntryIfPresent(name).map(entry -> entry.getStore());
                    if (stored.isEmpty()) {
                        throw new IllegalArgumentException(String.format("Store not found: %s", name));
                    }

                    if (!(stored.get() instanceof FileSystem)) {
                        throw new IllegalArgumentException(String.format("Store not a machine store: %s", name));
                    }

                    return stored.get();
                });
    }

    public static Dialog dataStoreFlowQuery(DataFlow flow, DataFlow[] available) {
        return Dialog.choice("Flow", (DataFlow o) -> o.getDisplayName(), true, false, flow, available);
    }

    public static Dialog shellQuery(String displayName, DataStore store) {
        var storeName = DataStorage.get().getStoreDisplayName(store).orElse("localhost");
        return Dialog.query(displayName, false, true, false, storeName, QueryConverter.STRING)
                .map((String name) -> {
                    if (name.equals("local") || name.equals("localhost")) {
                        return new LocalStore();
                    }

                    var stored = DataStorage.get().getStoreEntryIfPresent(name).map(entry -> entry.getStore());
                    if (stored.isEmpty()) {
                        throw new IllegalArgumentException(String.format("Store not found: %s", name));
                    }

                    if (!(stored.get() instanceof ShellStore)) {
                        throw new IllegalArgumentException(String.format("Store not a shell store: %s", name));
                    }

                    return stored.get();
                });
    }

    public static Dialog charsetQuery(StreamCharset c, boolean preferQuiet) {
        return Dialog.query("Charset", false, true, c != null && preferQuiet, c, QueryConverter.CHARSET);
    }

    public static Dialog newLineQuery(NewLine n, boolean preferQuiet) {
        return Dialog.query("Newline", false, true, n != null && preferQuiet, n, QueryConverter.NEW_LINE);
    }

    public static <T> Dialog query(String desc, T value, boolean required, QueryConverter<T> c, boolean preferQuiet) {
        return Dialog.query(desc, false, required, value != null && preferQuiet, value, c);
    }

    public static Dialog booleanChoice(String desc, boolean value, boolean preferQuiet) {
        return Dialog.choice(desc, val -> val.toString(), true, preferQuiet, value, Boolean.TRUE, Boolean.FALSE);
    }

    public static Dialog fileQuery(String name) {
        return Dialog.query("File", true, true, false, name, QueryConverter.STRING);
    }

    public static Dialog userQuery(String name) {
        return Dialog.query("User", false, true, false, name, QueryConverter.STRING);
    }

    public static Dialog namedStoreQuery(DataStore store, Class<? extends DataStore> filter) {
        var name = DataStorage.get().getStoreDisplayName(store).orElse(null);
        return Dialog.query("Store", false, true, false, name, QueryConverter.STRING)
                .map((String newName) -> {
                    var found = DataStorage.get()
                            .getStoreEntryIfPresent(newName)
                            .map(entry -> entry.getStore())
                            .orElseThrow();
                    if (!filter.isAssignableFrom(found.getClass())) {
                        throw new IllegalArgumentException("Incompatible store type");
                    }
                    return found;
                });
    }

    public static Dialog passwordQuery(SecretValue password) {
        return Dialog.querySecret("Password", false, true, password);
    }

    public static Dialog timeoutQuery(Integer timeout) {
        return Dialog.query("Timeout", false, true, false, timeout, QueryConverter.INTEGER);
    }

    @Value
    public static class Address {
        String hostname;
        Integer port;
    }
}
