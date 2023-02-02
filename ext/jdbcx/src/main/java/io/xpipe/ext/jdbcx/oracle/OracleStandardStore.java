package io.xpipe.ext.jdbcx.oracle;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.core.store.ShellStore;
import io.xpipe.ext.jdbc.JdbcBaseStore;
import io.xpipe.ext.jdbc.JdbcDatabaseStore;
import io.xpipe.ext.jdbc.address.JdbcAddress;
import io.xpipe.ext.jdbc.auth.AuthMethod;
import io.xpipe.ext.jdbc.auth.SimpleAuthMethod;
import io.xpipe.ext.jdbc.auth.WindowsAuth;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.HashMap;
import java.util.Map;

@JsonTypeName("oracleStandard")
@SuperBuilder
@Jacksonized
public class OracleStandardStore extends JdbcDatabaseStore implements JdbcBaseStore {

    public OracleStandardStore(ShellStore proxy, JdbcAddress address, AuthMethod auth, String database) {
        super(proxy, address, auth, database);
    }

    @Override
    public String toUrl() {
        var base = "jdbc:oracle://" + address.toAddressString() + "/" + database;
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
