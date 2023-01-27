package io.xpipe.ext.jdbc;

import io.xpipe.core.store.ShellStore;
import io.xpipe.core.util.JacksonizedValue;
import io.xpipe.ext.jdbc.address.JdbcAddress;
import io.xpipe.ext.jdbc.auth.AuthMethod;
import io.xpipe.extension.util.Validators;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import java.sql.Connection;
import java.sql.SQLException;

@Getter
@SuperBuilder
public abstract class JdbcDatabaseStore extends JacksonizedValue implements JdbcBaseStore {

    protected final ShellStore proxy;
    protected final JdbcAddress address;
    protected final AuthMethod auth;
    protected final String database;

    protected JdbcDatabaseStore(ShellStore proxy, JdbcAddress address, AuthMethod auth, String database) {
        this.proxy = proxy;
        this.address = address;
        this.auth = auth;
        this.database = database;
    }

    @Override
    public void checkComplete() throws Exception {
        Validators.nonNull(proxy, "Proxy");
        Validators.nonNull(address, "Address");
        Validators.nonNull(auth, "Authentication Method");
        Validators.nonNull(database, "Database");
    }

    @Override
    public void validate() throws Exception {
        // XPipeProxy.checkSupport(proxy);
        JdbcBaseStore.super.validate();
    }

    @Override
    public Connection createConnection() throws SQLException {
        var connection = JdbcBaseStore.super.createConnection();
        connection.setCatalog(database);
        return connection;
    }

    public String getSelectedDatabase() {
        return database;
    }
}
