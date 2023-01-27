package io.xpipe.ext.jdbc.mysql;

import com.fasterxml.jackson.annotation.JsonTypeName;
import io.xpipe.ext.jdbc.JdbcBaseStore;
import io.xpipe.ext.jdbc.JdbcDatabaseServerStore;
import io.xpipe.ext.jdbc.auth.SimpleAuthMethod;
import io.xpipe.ext.jdbc.auth.WindowsAuth;
import lombok.experimental.SuperBuilder;
import lombok.extern.jackson.Jacksonized;

import java.util.HashMap;
import java.util.Map;

@JsonTypeName("mysqlSimple")
@SuperBuilder
@Jacksonized
public class MysqlSimpleStore extends JdbcDatabaseServerStore implements JdbcBaseStore {

    @Override
    public String toUrl() {
        var base = "jdbc:mysql://" + address.toAddressString();

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
