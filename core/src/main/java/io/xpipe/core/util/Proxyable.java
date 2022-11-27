package io.xpipe.core.util;

import io.xpipe.core.store.ShellStore;

public interface Proxyable {

    public static ShellStore getProxy(Object base) {
        var proxy = base instanceof Proxyable p ? p.getProxy() : null;
        return ShellStore.isLocal(proxy) ? null : proxy;
    }

    public static boolean isRemote(Object base) {
        if (base == null) {
            throw new IllegalArgumentException("Proxy base is null");
        }

        return base instanceof Proxyable p && !ShellStore.isLocal(p.getProxy());
    }

    ShellStore getProxy();
}
