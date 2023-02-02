import com.fasterxml.jackson.databind.Module;
import io.xpipe.ext.jdbc.JdbcDialect;
import io.xpipe.ext.jdbcx.JdbcxJacksonModule;
import io.xpipe.ext.jdbcx.mssql.MssqlDialect;
import io.xpipe.ext.jdbcx.mssql.MssqlStoreProvider;
import io.xpipe.ext.jdbcx.oracle.OracleStoreProvider;
import io.xpipe.extension.DataStoreProvider;

import java.sql.Driver;

open module io.xpipe.ext.jdbcx {
    exports io.xpipe.ext.jdbcx.mssql;

    requires io.xpipe.ext.jdbc;
    requires io.xpipe.core;
    requires io.xpipe.extension;
    requires static jarchivelib;
    requires static lombok;
    requires java.sql;
    requires com.fasterxml.jackson.databind;
    requires static net.synedra.validatorfx;
    requires javafx.base;
    requires javafx.graphics;
    requires com.microsoft.sqlserver.jdbc;
    requires io.xpipe.beacon;

    uses Driver;

    provides JdbcDialect with MssqlDialect;
    provides Module with
            JdbcxJacksonModule;
    provides DataStoreProvider with
            MssqlStoreProvider,
            OracleStoreProvider;
}
