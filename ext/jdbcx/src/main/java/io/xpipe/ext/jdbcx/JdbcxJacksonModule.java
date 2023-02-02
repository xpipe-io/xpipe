package io.xpipe.ext.jdbcx;

import com.fasterxml.jackson.databind.jsontype.NamedType;
import com.fasterxml.jackson.databind.module.SimpleModule;
import io.xpipe.ext.jdbcx.mssql.MssqlAddress;

public class JdbcxJacksonModule extends SimpleModule {

    @Override
    public void setupModule(SetupContext context) {
        context.registerSubtypes(
                new NamedType(MssqlAddress.class));
    }
}
