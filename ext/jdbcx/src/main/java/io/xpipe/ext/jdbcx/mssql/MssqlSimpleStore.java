package io.xpipe.ext.jdbcx.mssql;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.ShellStore;
import io.xpipe.ext.jdbc.JdbcDatabaseServerStore;
import io.xpipe.ext.jdbc.address.JdbcAddress;
import io.xpipe.ext.jdbc.auth.AuthMethod;
import io.xpipe.ext.jdbc.auth.SimpleAuthMethod;
import io.xpipe.ext.jdbc.auth.WindowsAuth;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.HashMap;
import java.util.Map;

@JsonTypeName("mssqlSimple")
@SuperBuilder
@Jacksonized
public class MssqlSimpleStore extends JdbcDatabaseServerStore implements MssqlStore {

    public MssqlSimpleStore(ShellStore proxy, JdbcAddress address, AuthMethod auth) {
        super(proxy, address, auth);
    }

    @Override
    public String toUrl() {
        var base =
                "jdbc:sqlserver://" + address.toAddressString() + ";encrypt=false;" + "trustServerCertificate=false;";
        if (auth instanceof WindowsAuth) {
            base = base + "integratedSecurity=true;";
        }
        return base;
    }

    @Override
    public Map<String, String> createProperties() {
        var p = new HashMap<String, String>();

        switch (auth) {
            case SimpleAuthMethod s -> {
                p.put("user", s.getUsername());
                p.put("password", s.getPassword().getSecretValue());
            }
            case WindowsAuth a -> {}
            default -> {}
        }

        return p;
    }
}
