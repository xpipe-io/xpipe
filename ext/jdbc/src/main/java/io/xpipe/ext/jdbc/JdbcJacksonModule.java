package io.xpipe.ext.jdbc;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.xpipe.ext.jdbc.address.JdbcBasicAddress;
import io.xpipe.ext.jdbc.auth.SimpleAuthMethod;
import io.xpipe.ext.jdbc.auth.WindowsAuth;
import io.xpipe.ext.jdbc.postgres.PostgresSimpleStore;
import io.xpipe.ext.jdbc.postgres.PostgresUrlStore;
import io.xpipe.ext.jdbc.source.JdbcDynamicQuerySource;
import io.xpipe.ext.jdbc.source.JdbcStaticQuerySource;
import io.xpipe.ext.jdbc.source.JdbcTableSource;

public class JdbcJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(
                new NamedType(JdbcStaticQuerySource.class),
                new NamedType(JdbcDynamicQuerySource.class),
                new NamedType(JdbcTableSource.class),
                new NamedType(SimpleAuthMethod.class),
                new NamedType(WindowsAuth.class),
                new NamedType(JdbcBasicAddress.class),
                new NamedType(PostgresSimpleStore.class),
                new NamedType(PostgresUrlStore.class));
    }
}
