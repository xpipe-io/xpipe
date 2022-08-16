package io.xpipe.extension;

import io.xpipe.core.dialog.Dialog;
import io.xpipe.core.dialog.QueryConverter;
import io.xpipe.core.store.DataStore;
import io.xpipe.core.store.LocalStore;
import io.xpipe.core.store.MachineFileStore;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.Secret;
import lombok.Value;

public class DialogHelper {

    @Value
    public static class Address {
        String hostname;
        Integer port;
    }

    public static Dialog addressQuery(Address address) {
        var hostNameQuery = Dialog.query("Hostname", false, true, false, address.getHostname(), QueryConverter.STRING);
        var portQuery = Dialog.query("Port", false, true, false, address.getPort(), QueryConverter.INTEGER);
        return Dialog.chain(hostNameQuery, portQuery).evaluateTo(() -> new Address(hostNameQuery.getResult(), portQuery.getResult()));
    }

    public static Dialog machineQuery(DataStore store) {
        var storeName = XPipeDaemon.getInstance().getStoreName(store).orElse("local");
        return Dialog.query("Machine", false, true, false, storeName, QueryConverter.STRING).map((String name) -> {
            if (name.equals("local")) {
                return new LocalStore();
            }

            var stored = XPipeDaemon.getInstance().getNamedStore(name);
            if (stored.isEmpty()) {
                throw new IllegalArgumentException(String.format("Store not found: %s", name));
            }

            if (!(stored.get() instanceof MachineFileStore)) {
                throw new IllegalArgumentException(String.format("Store not a machine store: %s", name));
            }

            return stored.get();
        });
    }

    public static Dialog shellQuery(DataStore store) {
        var storeName = XPipeDaemon.getInstance().getStoreName(store).orElse("local");
        return Dialog.query("Shell", false, true, false, storeName, QueryConverter.STRING).map((String name) -> {
            if (name.equals("local")) {
                return new LocalStore();
            }

            var stored = XPipeDaemon.getInstance().getNamedStore(name);
            if (stored.isEmpty()) {
                throw new IllegalArgumentException(String.format("Store not found: %s", name));
            }

            if (!(stored.get() instanceof ShellStore)) {
                throw new IllegalArgumentException(String.format("Store not a shell store: %s", name));
            }

            return stored.get();
        });
    }

    public static Dialog fileQuery(String name) {
        return Dialog.query("File", true, true, false, name, QueryConverter.STRING);
    }

    public static Dialog userQuery(String name) {
        return Dialog.query("User", false, true, false, name, QueryConverter.STRING);
    }


    public static Dialog passwordQuery(Secret password) {
        return Dialog.querySecret("Password", false, true, password);
    }

    public static Dialog timeoutQuery(Integer timeout) {
        return Dialog.query("Timeout", false, true, false, timeout, QueryConverter.INTEGER);
    }

}
