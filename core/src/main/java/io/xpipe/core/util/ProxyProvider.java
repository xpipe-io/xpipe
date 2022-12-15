package io.xpipe.core.util;

import io.xpipe.core.source.DataSource;
import io.xpipe.core.source.DataSourceConnection;
import io.xpipe.core.source.DataSourceReadConnection;
import io.xpipe.core.source.WriteMode;
import io.xpipe.core.store.ShellStore;

import java.util.ServiceLoader;

public abstract class ProxyProvider {

    private static ProxyProvider INSTANCE;

    public static ProxyProvider get() {
        if (INSTANCE == null) {
            INSTANCE = ServiceLoader.load(ModuleLayer.boot(), ProxyProvider.class)
                            .findFirst()
                            .orElseThrow();
        }

        return INSTANCE;
    }

    public abstract <T> T downstreamTransform(T object, ShellStore proxy);

    public abstract ShellStore getProxy(Object base);

    public abstract  boolean isRemote(Object base);

    public abstract <T extends DataSourceReadConnection> T createRemoteReadConnection(
            DataSource<?> source, ShellStore proxy) throws Exception;

    public abstract <T extends DataSourceConnection> T createRemoteWriteConnection(
            DataSource<?> source, WriteMode mode, ShellStore proxy) throws Exception;

    public abstract ProxyFunction call(ProxyFunction func, ShellStore proxy);
}
