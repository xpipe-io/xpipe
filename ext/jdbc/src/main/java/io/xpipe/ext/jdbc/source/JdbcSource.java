package io.xpipe.ext.jdbc.source;

import io.xpipe.core.source.TableDataSource;
import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.ProxyProvider;
import io.xpipe.core.util.Proxyable;
import io.xpipe.ext.jdbc.JdbcStore;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.sql.Connection;
import java.sql.SQLException;

@Getter
@SuperBuilder
public abstract class JdbcSource extends TableDataSource<JdbcStore> implements Proxyable {

    String database;

    @Override
    public ShellStore getProxy() {
        return ProxyProvider.get().getProxy(store);
    }

    protected void prepareConnection(Connection connection) throws SQLException {
        connection.setCatalog(database);
    }

    public Connection createConnection() throws SQLException {
        return store.createConnection();
    }
}
