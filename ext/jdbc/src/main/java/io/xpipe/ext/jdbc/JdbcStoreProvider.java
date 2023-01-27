package io.xpipe.ext.jdbc;

import io.xpipe.core.store.DataStore;
import io.xpipe.core.util.ProxyProvider;
import io.xpipe.extension.DataStoreProvider;
import io.xpipe.extension.util.DataStoreFormatter;

import java.sql.Connection;

public abstract class JdbcStoreProvider implements DataStoreProvider {

    private final String driverName;

    protected JdbcStoreProvider(String driverName) {
        this.driverName = driverName;
    }

    @Override
    public boolean isShareable() {
        return true;
    }

    @Override
    public boolean init() throws Exception {
        try {
            Class.forName(driverName);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    @Override
    public String queryInformationString(DataStore store, int length) throws Exception {
        JdbcStore s = store.asNeeded();
        try (Connection con = s.createConnection()) {
            return con.getMetaData().getDatabaseProductName() + " "
                    + con.getMetaData().getDatabaseProductVersion();
        }
    }

    @Override
    public String toSummaryString(DataStore store, int length) {
        String name;
        if (store instanceof JdbcDatabaseStore s && s.getAddress() != null) {
            name = s.getAddress().toDisplayString();
        } else if (store instanceof JdbcDatabaseServerStore s && s.getAddress() != null) {
            name = s.getAddress().toDisplayString();
        } else if (store instanceof JdbcUrlStore u) {
            name = u.getAddress();
        } else {
            name = "?";
        }

        var proxy = ProxyProvider.get().getProxy(store);
        if (proxy != null) {
            return DataStoreFormatter.formatViaProxy(value -> DataStoreFormatter.cut(name, value), proxy, length);
        } else {
            return DataStoreFormatter.cut(name, length);
        }
    }

    @Override
    public Category getCategory() {
        return Category.DATABASE;
    }
}
